package com.brianngo333.news.infrastructure.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.r2dbc.repository.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface NewsArticleRepository extends ReactiveCrudRepository<NewsArticleEntity, String> {
    Mono<NewsArticleEntity> findByUrl(String url);
    @Query("SELECT * FROM news_article ORDER BY published_at DESC NULLS LAST, first_seen_at DESC LIMIT 100")
    Flux<NewsArticleEntity> findLatest();
}
