package tech.lougon.profitly.stock.infrastructure.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tech.lougon.profitly.stock.application.service.StockQuoteService;
import tech.lougon.profitly.stock.infrastructure.client.BrapiStockClient;
import tech.lougon.profitly.stock.infrastructure.client.dto.BrapiTickerListResponse;

import java.util.ArrayList;
import java.util.List;

@Component
public class StockQuoteSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(StockQuoteSyncScheduler.class);
    private static final int BATCH_SIZE = 10;

    private final StockQuoteService stockService;
    private final BrapiStockClient brapiStockClient;

    public StockQuoteSyncScheduler(StockQuoteService stockService, BrapiStockClient brapiStockClient) {
        this.stockService = stockService;
        this.brapiStockClient = brapiStockClient;
    }

    @Scheduled(cron = "${profitly.scheduler.stock-sync-hourly-cron}")
    public void syncStocksEveryHour() {
        List<String> tickers = fetchAllStockTickers();
        log.info("Starting hourly stock sync for {} tickers", tickers.size());
        sync(tickers);
    }

    private List<String> fetchAllStockTickers() {
        return brapiStockClient.fetchTickerList().stocks().stream()
                .filter(t -> "stock".equalsIgnoreCase(t.type()))
                .map(BrapiTickerListResponse.BrapiTicker::stock)
                .toList();
    }

    private void sync(List<String> tickers) {
        int success = 0;
        int failure = 0;

        for (List<String> batch : chunk(tickers, BATCH_SIZE)) {
            try {
                int synced = stockService.syncManyFromBrapi(batch).size();
                success += synced;
                failure += batch.size() - synced;
                log.info("Synced batch: {}", batch);
            } catch (Exception e) {
                log.error("Failed to sync batch {}: {}", batch, e.getMessage());
                failure += batch.size();
            }
        }

        log.info("Stock sync finished — success: {}, failure: {}", success, failure);
    }

    private static List<List<String>> chunk(List<String> list, int size) {
        List<List<String>> chunks = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            chunks.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return chunks;
    }
}
