package com.brianngo333.news.api;

import com.brianngo333.news.application.NewsService;
import com.brianngo333.news.domain.CollectionStatus;
import com.brianngo333.news.domain.NewsArticle;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/v1")
public class NewsController {
    private final Logger log = LoggerFactory.getLogger(NewsController.class);
    private final NewsService service;
    public NewsController(NewsService service) { this.service = service; }

    @GetMapping("/news")
    public Mono<NewsResponse> latest(@RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        log.info("----> caller /news");
        return service.latest(Math.min(limit, 20)).collectList().map(items -> new NewsResponse(items, Instant.now(), items.size()));
    }

    @GetMapping("/news/{id}")
    public Mono<NewsArticle> byId(@PathVariable String id) {
        log.info("----> caller /news/{}", id);
        return service.findById(id).switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found")));
    }

    // Learning-only endpoint. Add authentication/authorization before exposing publicly.
    @GetMapping("/admin/collection/status")
    public Mono<CollectionStatus> status() { 
        log.info("----> caller /admin/collection/status");
        return service.status(); 
    }

    // Learning-only endpoint. Add authentication/authorization and rate limiting before public deployment.
    @PostMapping("/admin/collection/run")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<RunResponse> run() {
        log.info("----> caller /admin/collection/run");
        return service.collect().map(count -> new RunResponse("completed", count, Instant.now()))
                .onErrorResume(error -> Mono.just(new RunResponse("failed: " + error.getMessage(), 0, Instant.now())));
    }

    public record NewsResponse(List<NewsArticle> items, Instant responseAt, int count) { }
    public record RunResponse(String status, int processedCount, Instant completedAt) { }
}
