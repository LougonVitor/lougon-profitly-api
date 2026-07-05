package tech.lougon.profitly.analysis.infrastructure.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tech.lougon.profitly.analysis.infrastructure.client.BrapiAnalysisClient;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiTreasuryListResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiTreasuryHistoryResponse;
import tech.lougon.profitly.analysis.infrastructure.persistence.TreasuryBondJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.TreasuryBondHistoryJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaTreasuryBondRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaTreasuryBondHistoryRepository;
import tech.lougon.profitly.ticker.infrastructure.persistence.JpaTickerRepository;
import tech.lougon.profitly.ticker.infrastructure.persistence.TickerJpaEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class TreasurySyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(TreasurySyncScheduler.class);

    /** brapi caps /treasury/indicators/history at 20 symbols per call. */
    private static final int HISTORY_BATCH_SIZE = 20;
    private static final String BACKFILL_START_DATE = "2020-01-01";

    private final BrapiAnalysisClient brapiClient;
    private final JpaTreasuryBondRepository bondRepo;
    private final JpaTreasuryBondHistoryRepository historyRepo;
    private final JpaTickerRepository tickerRepo;

    public TreasurySyncScheduler(BrapiAnalysisClient brapiClient,
                                  JpaTreasuryBondRepository bondRepo,
                                  JpaTreasuryBondHistoryRepository historyRepo,
                                  JpaTickerRepository tickerRepo) {
        this.brapiClient = brapiClient;
        this.bondRepo = bondRepo;
        this.historyRepo = historyRepo;
        this.tickerRepo = tickerRepo;
    }

    @Value("${profitly.sync.on-startup:false}")
    private boolean syncOnStartup;

    // Dev-only flag while the treasury screen is under construction — turn off when done
    @Value("${profitly.sync.treasury-on-startup:false}")
    private boolean treasurySyncOnStartup;

    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void syncOnStartup() {
        if (!syncOnStartup && !treasurySyncOnStartup) return;
        log.info("Running treasury startup sync");
        syncAll();
    }

    @Scheduled(cron = "0 45 19 * * *", zone = "America/Sao_Paulo")
    public void syncAll() {
        // /api/v2/treasury/list already returns current rates and prices — no separate indicators call needed
        List<BrapiTreasuryListResponse.TreasuryItem> list = brapiClient.fetchTreasuryList();
        if (list.isEmpty()) {
            log.warn("Treasury list returned 0 bonds — check brapi token/endpoint");
            return;
        }
        log.info("Syncing {} treasury bonds", list.size());

        int synced = 0;
        for (BrapiTreasuryListResponse.TreasuryItem item : list) {
            if (item.symbol() == null) continue;
            try {
                saveCurrent(item);
                upsertTicker(item);
                synced++;
            } catch (Exception e) {
                log.warn("Failed to save treasury bond {}: {}", item.symbol(), e.getMessage());
            }
        }
        log.info("Treasury sync complete: {}/{}", synced, list.size());

        List<String> symbols = list.stream()
                .map(BrapiTreasuryListResponse.TreasuryItem::symbol)
                .filter(s -> s != null)
                .toList();
        syncAllHistories(symbols);
        updateTickerDailyChanges(symbols);
    }

    /**
     * Treasury quotes carry no daily change, so the hero variation is derived from the
     * last two buyPrice history entries and stored on the ticker like other assets.
     */
    private void updateTickerDailyChanges(List<String> symbols) {
        for (String symbol : symbols) {
            List<TreasuryBondHistoryJpaEntity> last2 =
                    historyRepo.findTop2BySymbolOrderByReferenceDateDesc(symbol);
            if (last2.size() < 2) continue;
            Double current = last2.get(0).getBuyPrice();
            Double previous = last2.get(1).getBuyPrice();
            if (current == null || previous == null || previous <= 0) continue;

            tickerRepo.findBySymbol(symbol).ifPresent(ticker -> {
                ticker.setChangePercent(BigDecimal.valueOf((current - previous) / previous * 100.0));
                tickerRepo.save(ticker);
            });
        }
    }

    private void saveCurrent(BrapiTreasuryListResponse.TreasuryItem item) {
        TreasuryBondJpaEntity entity = bondRepo.findById(item.symbol())
                .orElseGet(() -> { var e = new TreasuryBondJpaEntity(); e.setSymbol(item.symbol()); return e; });

        // bondType is the human-readable name (e.g. "Tesouro IPCA+ com Juros Semestrais")
        entity.setName(item.bondType());
        entity.setBondType(item.bondType());
        entity.setIndexer(item.indexer());
        entity.setCouponType(item.couponType());
        entity.setMaturityDate(item.maturityDate());
        entity.setDurationDays(item.durationDays());
        entity.setBaseDate(item.baseDate());
        entity.setBuyRate(item.buyRate());
        entity.setSellRate(item.sellRate());
        entity.setBuyPrice(item.buyPrice());
        entity.setSellPrice(item.sellPrice());
        entity.setBasePrice(item.basePrice());
        if (item.rateInfo() != null) {
            entity.setRateType(item.rateInfo().rateType());
            entity.setRateUnit(item.rateInfo().rateUnit());
            entity.setRateDescription(item.rateInfo().description());
        }
        entity.setSyncedAt(Instant.now());

        bondRepo.save(entity);
    }

    private void upsertTicker(BrapiTreasuryListResponse.TreasuryItem item) {
        TickerJpaEntity ticker = tickerRepo.findBySymbol(item.symbol())
                .orElseGet(TickerJpaEntity::new);

        // e.g. "Tesouro IPCA+ 2029" — official naming year makes bonds distinguishable in search
        String name = item.bondType() != null ? item.bondType() : item.symbol();
        if (item.bondType() != null && item.maturityDate() != null && item.maturityDate().length() >= 4) {
            try {
                int year = Integer.parseInt(item.maturityDate().substring(0, 4));
                name = item.bondType() + " " + (year - incomeYearsBeforeMaturity(item.bondType()));
            } catch (NumberFormatException e) {
                name = item.bondType();
            }
        }
        // sub_type column is varchar(30) — store only the indexer code (e.g. "ipca", "selic")
        String subType = item.indexer() != null ? item.indexer() : null;

        ticker.setSymbol(item.symbol());
        ticker.setName(name);
        ticker.setLongName(name);
        ticker.setAssetType("treasury");
        ticker.setSubType(subType);
        ticker.setIsActive(true);
        if (item.buyPrice() != null) {
            ticker.setLastPrice(BigDecimal.valueOf(item.buyPrice()));
        }
        ticker.setSyncedAt(Instant.now());
        tickerRepo.save(ticker);
    }

    /**
     * Renda+/Educa+ official names use the year the income phase STARTS, while brapi's
     * maturityDate is the last payment: Renda+ pays 240 monthly installments (first one
     * 19 years before maturity), Educa+ pays 60 (4 years before). Ex.: maturity 2084 ⇒
     * "Tesouro Renda+ Aposentadoria Extra 2065".
     */
    private static int incomeYearsBeforeMaturity(String bondType) {
        String t = bondType.toLowerCase();
        if (t.contains("renda+")) return 19;
        if (t.contains("educa+")) return 4;
        return 0;
    }

    /**
     * Syncs the daily rate/price series of every bond. Bonds without stored history get
     * a full backfill since 2020; the rest only fetch the last 3 months. Both paths run
     * in batches of up to 20 symbols per brapi call.
     */
    private void syncAllHistories(List<String> symbols) {
        List<String> backfill = new ArrayList<>();
        List<String> incremental = new ArrayList<>();
        for (String symbol : symbols) {
            if (historyRepo.countBySymbol(symbol) == 0) backfill.add(symbol);
            else incremental.add(symbol);
        }
        log.info("Treasury history sync: {} backfill, {} incremental", backfill.size(), incremental.size());

        int saved = 0;
        saved += syncHistoryBatches(backfill, BACKFILL_START_DATE);
        saved += syncHistoryBatches(incremental, LocalDate.now().minusMonths(3).toString());
        log.info("Treasury history sync complete: {} new entries", saved);
    }

    private int syncHistoryBatches(List<String> symbols, String startDate) {
        int saved = 0;
        for (int i = 0; i < symbols.size(); i += HISTORY_BATCH_SIZE) {
            List<String> batch = symbols.subList(i, Math.min(i + HISTORY_BATCH_SIZE, symbols.size()));
            try {
                List<BrapiTreasuryHistoryResponse.TreasuryHistoryResult> results =
                        brapiClient.fetchTreasuryHistory(String.join(",", batch), startDate, null);
                for (var result : results) {
                    saved += saveHistory(result);
                }
            } catch (Exception e) {
                log.warn("Failed to sync treasury history batch [{}...]: {}", batch.get(0), e.getMessage());
            }
        }
        return saved;
    }

    private int saveHistory(BrapiTreasuryHistoryResponse.TreasuryHistoryResult result) {
        if (result.history() == null || result.history().isEmpty()) return 0;

        Set<String> existingDates = historyRepo.findBySymbolOrderByReferenceDateAsc(result.symbol())
                .stream().map(TreasuryBondHistoryJpaEntity::getReferenceDate)
                .collect(Collectors.toSet());

        Instant now = Instant.now();
        List<TreasuryBondHistoryJpaEntity> toSave = new ArrayList<>();
        for (var e : result.history()) {
            if (e.baseDate() == null) continue;
            String refDate = e.baseDate().length() > 10 ? e.baseDate().substring(0, 10) : e.baseDate();
            if (!existingDates.add(refDate)) continue;

            var entity = new TreasuryBondHistoryJpaEntity();
            entity.setSymbol(result.symbol());
            entity.setReferenceDate(refDate);
            entity.setBuyRate(e.buyRate());
            entity.setSellRate(e.sellRate());
            entity.setBuyPrice(e.buyPrice());
            entity.setSellPrice(e.sellPrice());
            entity.setBasePrice(e.basePrice());
            entity.setSyncedAt(now);
            toSave.add(entity);
        }
        historyRepo.saveAll(toSave);
        return toSave.size();
    }
}
