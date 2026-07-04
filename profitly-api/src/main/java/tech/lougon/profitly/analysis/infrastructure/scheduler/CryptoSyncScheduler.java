package tech.lougon.profitly.analysis.infrastructure.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tech.lougon.profitly.analysis.infrastructure.client.BrapiAnalysisClient;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiCryptoResponse;
import tech.lougon.profitly.analysis.infrastructure.persistence.CryptoCoinJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.CryptoQuoteJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaCryptoCoinRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaCryptoQuoteRepository;
import tech.lougon.profitly.ticker.infrastructure.persistence.JpaTickerRepository;
import tech.lougon.profitly.ticker.infrastructure.persistence.TickerJpaEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Syncs crypto data from brapi:
 *  1. /api/v2/crypto/available — catalog of coin symbols (crypto_coins table)
 *  2. /api/v2/crypto?coin=...&currency=BRL — quote data in batches (crypto_quotes table)
 */
@Component
public class CryptoSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(CryptoSyncScheduler.class);
    private static final int BATCH_SIZE = 20;

    private final BrapiAnalysisClient brapiClient;
    private final JpaCryptoCoinRepository coinRepo;
    private final JpaCryptoQuoteRepository quoteRepo;
    private final JpaTickerRepository tickerRepo;

    public CryptoSyncScheduler(BrapiAnalysisClient brapiClient,
                                JpaCryptoCoinRepository coinRepo,
                                JpaCryptoQuoteRepository quoteRepo,
                                JpaTickerRepository tickerRepo) {
        this.brapiClient = brapiClient;
        this.coinRepo = coinRepo;
        this.quoteRepo = quoteRepo;
        this.tickerRepo = tickerRepo;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void syncOnStartup() {
        log.info("Running crypto startup sync");
        syncAll();
    }

    @Scheduled(cron = "0 50 19 * * *", zone = "America/Sao_Paulo")
    public void syncAll() {
        // Step 1: coin catalog
        List<String> coins = brapiClient.fetchCryptoAvailable();
        if (coins.isEmpty()) {
            log.warn("Crypto available list returned 0 coins — skipping crypto sync");
            return;
        }

        Instant now = Instant.now();
        for (String coin : coins) {
            coinRepo.save(new CryptoCoinJpaEntity(coin, now));
        }
        log.info("Crypto coin catalog synced: {} coins", coins.size());

        // Step 2: quotes in batches of 20
        int quotesSynced = 0;
        for (List<String> batch : partition(coins, BATCH_SIZE)) {
            List<BrapiCryptoResponse.CryptoQuote> quotes =
                    brapiClient.fetchCryptoQuotes(String.join(",", batch));
            for (BrapiCryptoResponse.CryptoQuote quote : quotes) {
                try {
                    saveQuote(quote);
                    upsertTicker(quote);
                    quotesSynced++;
                } catch (Exception e) {
                    log.warn("Failed to save crypto quote for {}: {}", quote.coin(), e.getMessage());
                }
            }
        }
        log.info("Crypto sync complete: {}/{} quotes synced", quotesSynced, coins.size());
    }

    private void saveQuote(BrapiCryptoResponse.CryptoQuote quote) {
        CryptoQuoteJpaEntity entity = quoteRepo.findById(quote.coin())
                .orElseGet(() -> { var e = new CryptoQuoteJpaEntity(); e.setCoin(quote.coin()); return e; });

        entity.setCoinName(quote.coinName());
        entity.setCurrency(quote.currency());
        entity.setImageUrl(quote.coinImageUrl());
        entity.setPrice(quote.regularMarketPrice());
        entity.setChangePercent(quote.regularMarketChangePercent());
        entity.setDayHigh(quote.regularMarketDayHigh());
        entity.setDayLow(quote.regularMarketDayLow());
        entity.setVolume(quote.regularMarketVolume());
        entity.setMarketCap(quote.marketCap());
        entity.setMarketTime(quote.regularMarketTime());
        entity.setSyncedAt(Instant.now());
        quoteRepo.save(entity);
    }

    /** Upserts the coin into the tickers table so it appears in search. */
    private void upsertTicker(BrapiCryptoResponse.CryptoQuote quote) {
        TickerJpaEntity ticker = tickerRepo.findBySymbol(quote.coin())
                .orElseGet(TickerJpaEntity::new);

        String name = quote.coinName() != null ? quote.coinName() : quote.coin();
        ticker.setSymbol(quote.coin());
        ticker.setName(name);
        ticker.setLongName(name);
        ticker.setAssetType("crypto");
        ticker.setSubType("crypto");
        ticker.setCurrency(quote.currency());
        ticker.setLogoUrl(quote.coinImageUrl());
        ticker.setIsActive(true);
        if (quote.regularMarketPrice() != null) {
            ticker.setLastPrice(BigDecimal.valueOf(quote.regularMarketPrice()));
        }
        ticker.setSyncedAt(Instant.now());
        tickerRepo.save(ticker);
    }

    private static <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            result.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return result;
    }
}
