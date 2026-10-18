package com.brianngo333.news.infrastructure.rss;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLConnection;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;


@Component
public class RssFeedClient {
    private static final Logger log = LoggerFactory.getLogger(RssFeedClient.class);
    private final String feedUrl;
    private final int connectTimeoutMs;
    private final int responseTimeoutMs;

    public RssFeedClient(@Value("${news.rss.url}") String feedUrl,
                         @Value("${news.rss.connect-timeout-ms:5000}") int connectTimeoutMs,
                         @Value("${news.rss.response-timeout-ms:10000}") int responseTimeoutMs) {
        this.feedUrl = feedUrl;
        this.connectTimeoutMs = connectTimeoutMs;
        this.responseTimeoutMs = responseTimeoutMs;
        log.info("RssFeedClient initialized with feedUrl={}, connectTimeoutMs={}, responseTimeoutMs={}", feedUrl, connectTimeoutMs, responseTimeoutMs);
    }

    public Mono<List<FeedArticle>> fetch() {
        return Mono.fromCallable(this::fetchBlocking).subscribeOn(Schedulers.boundedElastic());
    }

    private List<FeedArticle> fetchBlocking() throws Exception {
        URLConnection connection = URI.create(feedUrl).toURL().openConnection();
        connection.setConnectTimeout(connectTimeoutMs);
        connection.setReadTimeout(responseTimeoutMs);
        connection.setRequestProperty("User-Agent", "VnExpressNewsLearningApp/1.0 (RSS reader)");
        try (XmlReader reader = new XmlReader(connection)) {
            SyndFeed feed = new SyndFeedInput().build(reader);
            String feedTitle = Optional.ofNullable(feed.getTitle()).filter(s -> !s.isBlank()).orElse("VnExpress");
            return feed.getEntries().stream().map(entry -> toArticle(entry, feedTitle)).toList();
        }
    }

    private FeedArticle toArticle(SyndEntry entry, String feedTitle) {
        String url = Optional.ofNullable(entry.getLink()).orElse("").trim();
        String title = Optional.ofNullable(entry.getTitle()).orElse("").trim();
        String idSeed = !url.isBlank() ? url : title + "|" + Optional.ofNullable(entry.getPublishedDate()).map(Date::toInstant).orElse(Instant.EPOCH);
        Instant published = Optional.ofNullable(entry.getPublishedDate()).map(Date::toInstant)
                .or(() -> Optional.ofNullable(entry.getUpdatedDate()).map(Date::toInstant)).orElse(null);
        String summary = Optional.ofNullable(entry.getDescription()).map(d -> d.getValue()).orElse(null);
        return new FeedArticle(idSeed, title, feedTitle, url, summary, null, published);
    }

    public record FeedArticle(String idSeed, String title, String source, String url, String summary, String category, Instant publishedAt) { }
}
