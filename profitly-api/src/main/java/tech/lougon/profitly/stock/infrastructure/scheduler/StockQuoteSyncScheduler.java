package tech.lougon.profitly.stock.infrastructure.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public void syncFiis() {
        List<BrapiFiiListResponse.BrapiFii> fiis = brapiStockClient.fetchAllFiis().stream()
                .filter(f -> f.symbol() != null && !f.symbol().isBlank())
                .toList();
        log.info("Syncing {} FIIs from list (skipped nulls)", fiis.size());

        int success = 0;
        int skipped = 0;
        int failure = 0;

        for (BrapiFiiListResponse.BrapiFii fii : fiis) {
            if (fii.price() == null) {
                skipped++;
                continue;
            }
            try {
                stockService.syncFiiFromList(fii);
                success++;
            } catch (Exception e) {
                log.warn("Failed to sync FII {} ({}): {}", fii.symbol(), fii.name(), e.getMessage());
                failure++;
            }
        }

        log.info("FII sync finished — success: {}, skipped (no price): {}, failure: {}", success, skipped, failure);
    }
}
