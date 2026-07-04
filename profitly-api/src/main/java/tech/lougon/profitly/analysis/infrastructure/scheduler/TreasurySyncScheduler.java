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
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiTreasuryIndicatorsResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiTreasuryHistoryResponse;
import tech.lougon.profitly.analysis.infrastructure.persistence.TreasuryBondJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.TreasuryBondHistoryJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaTreasuryBondRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaTreasuryBondHistoryRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class TreasurySyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(TreasurySyncScheduler.class);
    private static final int BATCH_SIZE = 20;

    private final BrapiAnalysisClient brapiClient;
    private final JpaTreasuryBondRepository bondRepo;
    private final JpaTreasuryBondHistoryRepository historyRepo;

    public TreasurySyncScheduler(BrapiAnalysisClient brapiClient,
                                  JpaTreasuryBondRepository bondRepo,
                                  JpaTreasuryBondHistoryRepository historyRepo) {
        this.brapiClient = brapiClient;
        this.bondRepo = bondRepo;
        this.historyRepo = historyRepo;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void syncOnStartup() {
        if (bondRepo.count() == 0) {
            log.info("treasury_bonds table is empty — running initial treasury sync");
            syncAll();
        }
    }

    @Scheduled(cron = "0 45 19 * * *", zone = "America/Sao_Paulo")
    public void syncAll() {
        List<BrapiTreasuryListResponse.TreasuryItem> list = brapiClient.fetchTreasuryList();
        if (list.isEmpty()) {
            log.warn("Treasury list returned 0 bonds");
            return;
        }
        log.info("Syncing {} treasury bonds", list.size());

        // Build symbol→listItem map for metadata
        Map<String, BrapiTreasuryListResponse.TreasuryItem> metaMap = list.stream()
                .collect(Collectors.toMap(BrapiTreasuryListResponse.TreasuryItem::symbol, i -> i, (a, b) -> a));

        // Fetch current indicators in batches
        List<String> symbols = list.stream().map(BrapiTreasuryListResponse.TreasuryItem::symbol).toList();
        List<List<String>> batches = partition(symbols, BATCH_SIZE);

        int synced = 0;
        for (List<String> batch : batches) {
            try {
                String joined = String.join(",", batch);
                List<BrapiTreasuryIndicatorsResponse.TreasuryIndicator> indicators =
                        brapiClient.fetchTreasuryIndicators(joined);

                for (BrapiTreasuryIndicatorsResponse.TreasuryIndicator ind : indicators) {
                    if (ind.symbol() == null) continue;
                    try {
                        saveCurrent(ind, metaMap.get(ind.symbol()));
                        synced++;
                    } catch (Exception e) {
                        log.warn("Failed to save treasury bond {}: {}", ind.symbol(), e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.warn("Treasury batch indicators failed for batch {}: {}", batch, e.getMessage());
            }
        }

        log.info("Treasury current indicators synced: {}/{}", synced, symbols.size());

        // Sync history for each bond
        for (String symbol : symbols) {
            try {
                syncHistory(symbol);
            } catch (Exception e) {
                log.warn("Treasury history sync failed for {}: {}", symbol, e.getMessage());
            }
        }

        log.info("Treasury sync complete");
    }

    private void saveCurrent(BrapiTreasuryIndicatorsResponse.TreasuryIndicator ind,
                              BrapiTreasuryListResponse.TreasuryItem meta) {
        TreasuryBondJpaEntity entity = bondRepo.findById(ind.symbol())
                .orElseGet(() -> { var e = new TreasuryBondJpaEntity(); e.setSymbol(ind.symbol()); return e; });

        if (meta != null) {
            entity.setName(meta.name());
            entity.setBondType(meta.type());
            entity.setIndexer(meta.indexer());
            entity.setCouponType(meta.couponType());
            entity.setMaturityDate(meta.maturityDate());
        }

        entity.setBuyRate(ind.buyRate());
        entity.setSellRate(ind.sellRate());
        entity.setBuyPrice(ind.buyPrice());
        entity.setSellPrice(ind.sellPrice());
        entity.setBasePrice(ind.basePrice());
        entity.setDurationDays(ind.duration());
        entity.setSyncedAt(Instant.now());

        bondRepo.save(entity);
    }

    private void syncHistory(String symbol) {
        boolean firstSync = historyRepo.countBySymbol(symbol) == 0;
        String startDate = firstSync ? "2016-01-01" : LocalDate.now().minusMonths(3).toString();

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

    private static <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            result.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return result;
    }
}
