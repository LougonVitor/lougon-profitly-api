package tech.lougon.profitly.analysis.infrastructure.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class TreasurySyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(TreasurySyncScheduler.class);

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

    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void syncOnStartup() {
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
        entity.setBuyRate(item.buyRate());
        entity.setSellRate(item.sellRate());
        entity.setBuyPrice(item.buyPrice());
        entity.setSellPrice(item.sellPrice());
        entity.setBasePrice(item.basePrice());
        entity.setSyncedAt(Instant.now());

        bondRepo.save(entity);
    }

    private void upsertTicker(BrapiTreasuryListResponse.TreasuryItem item) {
        TickerJpaEntity ticker = tickerRepo.findBySymbol(item.symbol())
                .orElseGet(TickerJpaEntity::new);

        String name = item.bondType() != null ? item.bondType() : item.symbol();
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

    private void syncHistory(String symbol) {
        boolean firstSync = historyRepo.countBySymbol(symbol) == 0;
        String startDate = firstSync ? "2020-01-01" : LocalDate.now().minusMonths(3).toString();

        List<BrapiTreasuryHistoryResponse.TreasuryHistoryEntry> entries =
                brapiClient.fetchTreasuryHistory(symbol, startDate, null);
        if (entries.isEmpty()) return;

        Set<String> existingDates = historyRepo.findBySymbolOrderByReferenceDateAsc(symbol)
                .stream().map(TreasuryBondHistoryJpaEntity::getReferenceDate)
                .collect(Collectors.toSet());

        Instant now = Instant.now();
        for (var e : entries) {
            if (e.referenceDate() == null) continue;
            String refDate = e.referenceDate().length() > 10
                    ? e.referenceDate().substring(0, 10) : e.referenceDate();
            if (existingDates.contains(refDate)) continue;

            var entity = new TreasuryBondHistoryJpaEntity();
            entity.setSymbol(symbol);
            entity.setReferenceDate(refDate);
            entity.setBuyRate(e.buyRate());
            entity.setSellRate(e.sellRate());
            entity.setBuyPrice(e.buyPrice());
            entity.setSellPrice(e.sellPrice());
            entity.setBasePrice(e.basePrice());
            entity.setSyncedAt(now);
            historyRepo.save(entity);
            existingDates.add(refDate);
        }
    }
}
