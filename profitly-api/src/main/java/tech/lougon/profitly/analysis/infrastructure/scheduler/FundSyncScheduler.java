package tech.lougon.profitly.analysis.infrastructure.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tech.lougon.profitly.analysis.infrastructure.client.BrapiAnalysisClient;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiFundListResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiFundDividendsResponse;
import tech.lougon.profitly.analysis.infrastructure.persistence.DividendEventJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.FundIndicatorJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaDividendEventRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaFundIndicatorRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class FundSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(FundSyncScheduler.class);
    private static final int BATCH_SIZE = 20;

    // Fund types to sync from brapi
    private static final List<String> FUND_TYPES = List.of("FIAGRO", "FIDC", "FIP", "FI-Infra");

    private final BrapiAnalysisClient brapiClient;
    private final JpaFundIndicatorRepository fundRepo;
    private final JpaDividendEventRepository dividendRepo;

    public FundSyncScheduler(BrapiAnalysisClient brapiClient,
                              JpaFundIndicatorRepository fundRepo,
                              JpaDividendEventRepository dividendRepo) {
        this.brapiClient = brapiClient;
        this.fundRepo = fundRepo;
        this.dividendRepo = dividendRepo;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void syncOnStartup() {
        if (fundRepo.count() == 0) {
            log.info("fund_indicators table is empty — running initial fund sync");
            syncAll();
        }
    }

    @Scheduled(cron = "0 40 19 * * *", zone = "America/Sao_Paulo")
    public void syncAll() {
        // Fetch all funds from brapi (no type filter at request level, filter response)
        List<BrapiFundListResponse.FundItem> allFunds = brapiClient.fetchFundList("");
        if (allFunds.isEmpty()) {
            log.warn("Fund list returned 0 results — skipping fund sync");
            return;
        }

        log.info("Syncing {} funds", allFunds.size());

        Set<String> synced = new java.util.LinkedHashSet<>();
        for (BrapiFundListResponse.FundItem item : allFunds) {
            if (item.symbol() == null) continue;
            try {
                saveCurrent(item);
                synced.add(item.symbol());
            } catch (Exception e) {
                log.warn("Failed to save fund indicator for {}: {}", item.symbol(), e.getMessage());
            }
        }

        log.info("Fund indicators synced: {}/{}", synced.size(), allFunds.size());

        for (String symbol : synced) {
            try {
                syncDividends(symbol);
            } catch (Exception e) {
                log.warn("Fund dividend sync failed for {}: {}", symbol, e.getMessage());
            }
        }

        log.info("Fund sync complete");
    }

    private void saveCurrent(BrapiFundListResponse.FundItem item) {
        FundIndicatorJpaEntity entity = fundRepo.findById(item.symbol())
                .orElseGet(() -> { var e = new FundIndicatorJpaEntity(); e.setSymbol(item.symbol()); return e; });

        entity.setName(item.name());
        entity.setFundType(item.type());
        entity.setPrice(item.price());
        entity.setDividendYield12m(item.dividendYield12m());
        entity.setDividendYield1m(item.dividendYield1m());
        entity.setPriceToNav(item.priceToNav());
        entity.setNavPerShare(item.navPerShare());
        entity.setAdminName(item.administratorName());
        entity.setAdminCnpj(item.administratorCnpj());
        entity.setSegmentType(item.segmentType());
        if (item.totalInvestors() != null) {
            entity.setTotalInvestors(item.totalInvestors().longValue());
        }
        entity.setSyncedAt(Instant.now());
        fundRepo.save(entity);
    }

    @Transactional
    void syncDividends(String symbol) {
        List<BrapiFundDividendsResponse.FundDividend> dividends = brapiClient.fetchFundDividends(symbol);
        if (dividends.isEmpty()) return;

        dividendRepo.deleteBySymbol(symbol);

        for (var d : dividends) {
            var entity = new DividendEventJpaEntity();
            entity.setSymbol(symbol);
            entity.setLabel(d.label());
            entity.setRate(d.rate());
            entity.setPaymentDate(d.paymentDate());
            entity.setLastDatePrior(d.lastDatePrior());
            entity.setApprovedOn(d.approvedOn());
            entity.setRelatedTo(d.relatedTo());
            entity.setRemarks(d.remarks());
            dividendRepo.save(entity);
        }
    }
}
