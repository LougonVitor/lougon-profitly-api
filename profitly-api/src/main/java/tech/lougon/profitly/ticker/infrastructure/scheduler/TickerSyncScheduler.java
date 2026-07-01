package tech.lougon.profitly.ticker.infrastructure.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tech.lougon.profitly.ticker.application.service.TickerService;

@Component
public class TickerSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(TickerSyncScheduler.class);
    private final TickerService tickerService;

    public TickerSyncScheduler(TickerService tickerService) {
        this.tickerService = tickerService;
    }

    @Scheduled(cron = "0 0 19 * * *", zone = "America/Sao_Paulo")
    public void scheduledSync() {
        log.info("Daily 19h ticker sync started");
        tickerService.syncAll();
        log.info("Daily 19h ticker sync finished");
    }
}
