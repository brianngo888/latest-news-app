package com.brianngo333.news.domain;

import java.time.Instant;

public record CollectionStatus(
        Instant lastAttemptAt,
        Instant lastSuccessAt,
        String lastError,
        int lastItemCount) { }
