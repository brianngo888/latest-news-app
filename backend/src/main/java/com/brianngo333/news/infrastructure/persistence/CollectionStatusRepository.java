package com.brianngo333.news.infrastructure.persistence;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Repository
public class CollectionStatusRepository {
    private final DatabaseClient db;
    public CollectionStatusRepository(DatabaseClient db) { this.db = db; }

    public Mono<Void> attempted(Instant at) {
        return db.sql("UPDATE collection_status SET last_attempt_at=:at WHERE id=1")
                .bind("at", at.atOffset(ZoneOffset.UTC)).fetch().rowsUpdated().then();
    }
    public Mono<Void> succeeded(Instant at, int count) {
        return db.sql("UPDATE collection_status SET last_success_at=:at, last_error=NULL, last_item_count=:count WHERE id=1")
                .bind("at", at.atOffset(ZoneOffset.UTC)).bind("count", count).fetch().rowsUpdated().then();
    }
    public Mono<Void> failed(String error) {
        String safe = error == null ? "Unknown collection error" : error.substring(0, Math.min(error.length(), 1000));
        return db.sql("UPDATE collection_status SET last_error=:error WHERE id=1")
                .bind("error", safe).fetch().rowsUpdated().then();
    }
    public Mono<StatusRow> get() {
        return db.sql("SELECT last_attempt_at, last_success_at, last_error, last_item_count FROM collection_status WHERE id=1")
                .map((row, meta) -> new StatusRow(toInstant(row.get("last_attempt_at", OffsetDateTime.class)), toInstant(row.get("last_success_at", OffsetDateTime.class)), row.get("last_error", String.class), row.get("last_item_count", Integer.class)))
                .one();
    }
    private Instant toInstant(OffsetDateTime value) { return value == null ? null : value.toInstant(); }
    public record StatusRow(Instant lastAttemptAt, Instant lastSuccessAt, String lastError, Integer lastItemCount) { }
}
