package tech.lougon.profitly.news.infrastructure.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        log.info("Startup news sync triggered");
        newsService.sync();
    }

    @Scheduled(cron = "0 */30 * * * *")
    public void sync() {
        log.info("Scheduled news sync triggered");
        newsService.sync();
    }
}
