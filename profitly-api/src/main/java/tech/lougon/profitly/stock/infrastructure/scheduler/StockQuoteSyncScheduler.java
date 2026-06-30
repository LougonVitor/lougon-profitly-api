package tech.lougon.profitly.stock.infrastructure.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tech.lougon.profitly.stock.application.service.StockQuoteService;
import tech.lougon.profitly.stock.infrastructure.client.BrapiStockClient;
import tech.lougon.profitly.stock.infrastructure.client.dto.BrapiFiiListResponse;

import java.util.List;

@Component
public class StockQuoteSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(StockQuoteSyncScheduler.class);
    private final StockQuoteService stockService;
    private final BrapiStockClient brapiStockClient;

    public StockQuoteSyncScheduler(StockQuoteService stockService, BrapiStockClient brapiStockClient) {
        this.stockService = stockService;
        this.brapiStockClient = brapiStockClient;
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        log.info("Application ready — starting initial FII sync");
        syncFiis();
    }

    @Scheduled(cron = "${profitly.scheduler.stock-sync-hourly-cron}")
    public void syncEveryHour() {
        syncFiis();
    }

    private void syncFiis() {
        List<BrapiFiiListResponse.BrapiFii> fiis = brapiStockClient.fetchAllFiis();
        log.info("Syncing {} FIIs from list", fiis.size());

        int success = 0;
        int failure = 0;

        for (BrapiFiiListResponse.BrapiFii fii : fiis) {
            try {
                stockService.syncFiiFromList(fii);
                success++;
            } catch (Exception e) {
                log.warn("Failed to sync FII {} ({}): {}", fii.symbol(), fii.name(), e.getMessage());
                failure++;
            }
        }

        log.info("FII sync finished — success: {}, failure: {}", success, failure);
    }
}
