package com.brianngo333.news.application;

import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.brianngo333.news.domain.CollectionStatus;
import com.brianngo333.news.domain.NewsArticle;
import com.brianngo333.news.infrastructure.persistence.CollectionStatusRepository;
import com.brianngo333.news.infrastructure.persistence.NewsArticleEntity;
import com.brianngo333.news.infrastructure.persistence.NewsArticleRepository;
import com.brianngo333.news.infrastructure.rss.RssFeedClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;

@Service
public class NewsService {
    private static final Logger log = LoggerFactory.getLogger(NewsService.class);
    private final NewsArticleRepository articles;
    private final CollectionStatusRepository statuses;
    private final RssFeedClient feedClient;
    private final int maxArticles;
    private final AtomicBoolean collectionRunning = new AtomicBoolean(false);
    private final R2dbcEntityTemplate template;

    public NewsService(NewsArticleRepository articles, CollectionStatusRepository statuses, RssFeedClient feedClient,
                      @Value("${news.rss.max-articles:20}") int maxArticles, R2dbcEntityTemplate template) {
        this.articles = articles; 
        this.statuses = statuses; 
        this.feedClient = feedClient; 
        this.maxArticles = Math.max(1, maxArticles); 
        this.template = template;
    }

    public Flux<NewsArticle> latest(int requestedLimit) {
        int limit = Math.max(1, Math.min(requestedLimit, maxArticles));
        return articles.findLatest().take(limit).map(this::toDomain);
    }

    public Mono<NewsArticle> findById(String id) {
        return articles.findById(id).map(this::toDomain);
    }

    public Mono<CollectionStatus> status() {
        return statuses.get().map(s -> new CollectionStatus(s.lastAttemptAt(), s.lastSuccessAt(), s.lastError(), Objects.requireNonNullElse(s.lastItemCount(), 0)));
    }

    @Observed(name = "news.collection", contextualName = "collect-rss-news")
    public Mono<Integer> collect() {
        if (!collectionRunning.compareAndSet(false, true)) return Mono.error(new IllegalStateException("Collection already running"));
        Instant attemptAt = Instant.now();
        return statuses.attempted(attemptAt)
                .then(feedClient.fetch())
                .flatMapMany(Flux::fromIterable)
                .filter(item -> item.title() != null && !item.title().isBlank() && item.url() != null && validHttpUrl(item.url()))
                .concatMap(this::upsert)
                .take(maxArticles)
                .count()
                .map(Long::intValue)
                .flatMap(count -> statuses.succeeded(Instant.now(), count).thenReturn(count))
                .doOnSuccess(count -> log.info("RSS collection succeeded; processed {} items", count))
                .onErrorResume(error -> {
                    log.warn("RSS collection failed: {}", error.toString());
                    String message = error.getClass().getSimpleName() + ": " + error.getMessage();
                    return statuses.failed(message).then(Mono.error(error));
                })
                .doFinally(signal -> collectionRunning.set(false));
    }

    private Mono<NewsArticleEntity> upsert(RssFeedClient.FeedArticle item) {
        String id = sha256(item.idSeed());
        Instant now = Instant.now();
        return articles.findByUrl(item.url())
                .flatMap(existing -> articles.save(new NewsArticleEntity(existing.id(), item.title(), item.source(), item.url(), item.summary(), item.category(), item.publishedAt(), existing.firstSeenAt(), now)))
                .switchIfEmpty(Mono.defer(() -> {
                    NewsArticleEntity newEntity = new NewsArticleEntity(id, item.title(), item.source(), item.url(), item.summary(), item.category(), item.publishedAt(), now, now);
                    
                    return template.insert(NewsArticleEntity.class)
                        .using(newEntity);
                }));

                
    }

    

    private boolean validHttpUrl(String value) {
        try { URI uri = URI.create(value); return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) && uri.getHost() != null; }
        catch (Exception ex) { return false; }
    }
    private String sha256(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception ex) { throw new IllegalStateException("SHA-256 unavailable", ex); }
    }
    private NewsArticle toDomain(NewsArticleEntity e) {
        return new NewsArticle(e.id(), e.title(), e.source(), e.url(), e.summary(), e.category(), e.publishedAt(), e.firstSeenAt(), e.updatedAt());
    }
}
