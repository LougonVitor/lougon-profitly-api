package tech.lougon.profitly.news.infrastructure.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tech.lougon.profitly.news.application.service.NewsService;

@Component
public class NewsSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(NewsSyncScheduler.class);

    private final NewsService newsService;

    public NewsSyncScheduler(NewsService newsService) {
        this.newsService = newsService;
    }

    @Value("${profitly.sync.on-startup:false}")
    private boolean syncOnStartup;

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        if (!syncOnStartup) return;
        log.info("Startup news sync triggered");
        newsService.sync();
    }

    // News is independent of BRAPI, so it runs on its own cadence instead of the daily market pipeline.
    @Scheduled(cron = "0 0/30 * * * *", zone = "America/Sao_Paulo")
    public void sync() {
        log.info("Scheduled news sync triggered");
        newsService.sync();
    }
}
