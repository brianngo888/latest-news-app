package com.brianngo333.news.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CollectionScheduler {
    private static final Logger log = LoggerFactory.getLogger(CollectionScheduler.class);
    private final NewsService service;
    public CollectionScheduler(NewsService service) { this.service = service; }

    @EventListener(ApplicationReadyEvent.class)
    public void collectOnStartup() { run(); }

    @Scheduled(fixedDelayString = "${news.rss.interval-ms:300000}", initialDelayString = "${news.rss.interval-ms:300000}")
    public void scheduledCollection() { run(); }

    private void run() {
        service.collect().subscribe(ignored -> { }, error -> log.warn("Scheduled collection failed: {}", error.toString()));
    }
}
