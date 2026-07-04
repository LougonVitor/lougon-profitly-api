package tech.lougon.profitly.ticker.infrastructure.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tech.lougon.profitly.ticker.application.service.TickerService;
import tech.lougon.profitly.ticker.infrastructure.persistence.JpaTickerRepository;

@Component
public class TickerSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(TickerSyncScheduler.class);
    private final TickerService tickerService;
    private final JpaTickerRepository tickerRepo;

    public TickerSyncScheduler(TickerService tickerService, JpaTickerRepository tickerRepo) {
        this.tickerService = tickerService;
        this.tickerRepo = tickerRepo;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void syncOnStartup() {
        log.info("Running ticker startup sync");
        tickerService.syncAll();
        log.info("Ticker startup sync finished");
    }

    @Scheduled(cron = "0 0 19 * * *", zone = "America/Sao_Paulo")
    public void scheduledSync() {
        log.info("Daily 19h ticker sync started");
        tickerService.syncAll();
        log.info("Daily 19h ticker sync finished");
    }
}
