package com.brianngo333.news.infrastructure.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.time.Instant;

@Table("news_article")
public record NewsArticleEntity(
        @Id String id,
        String title,
        String source,
        String url,
        String summary,
        String category,
        Instant publishedAt,
        Instant firstSeenAt,
        Instant updatedAt) { }
