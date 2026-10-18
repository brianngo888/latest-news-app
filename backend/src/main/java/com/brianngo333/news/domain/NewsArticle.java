package com.brianngo333.news.domain;

import java.time.Instant;

public record NewsArticle(
        String id,
        String title,
        String source,
        String url,
        String summary,
        String category,
        Instant publishedAt,
        Instant firstSeenAt,
        Instant updatedAt) { }
