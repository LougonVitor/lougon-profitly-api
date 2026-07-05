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
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiFundListResponse;
import tech.lougon.profitly.analysis.infrastructure.persistence.FundIndicatorJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaFundIndicatorRepository;
import tech.lougon.profitly.ticker.infrastructure.persistence.JpaTickerRepository;
import tech.lougon.profitly.ticker.infrastructure.persistence.TickerJpaEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Syncs listed funds (FIAGRO, FI-Infra, FIDC, FIP) from /api/v2/funds/list.
 * One request per assetType with a high limit — no pagination needed.
 */
@Component
public class FundSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(FundSyncScheduler.class);

    /**
     * assetTypes fetched from the dedicated funds endpoint. FIIs come from /fii/list;
     * fidc and fip return nothing here, so they come from /api/v2/tickers?subType=... instead.
     */
    private static final List<String> ASSET_TYPES = List.of("fiagro", "fiinfra");

    private final BrapiAnalysisClient brapiClient;
    private final JpaFundIndicatorRepository fundRepo;
    private final JpaTickerRepository tickerRepo;

    public FundSyncScheduler(BrapiAnalysisClient brapiClient,
                              JpaFundIndicatorRepository fundRepo,
                              JpaTickerRepository tickerRepo) {
        this.brapiClient = brapiClient;
        this.fundRepo = fundRepo;
        this.tickerRepo = tickerRepo;
    }

    @Value("${profitly.sync.on-startup:false}")
    private boolean syncOnStartup;

    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void syncOnStartup() {
        if (!syncOnStartup) return;
        log.info("Running fund startup sync");
        syncAll();
    }

    @Scheduled(cron = "0 40 19 * * *", zone = "America/Sao_Paulo")
    public void syncAll() {
        int totalSynced = 0;
        for (String assetType : ASSET_TYPES) {
            List<BrapiFundListResponse.FundItem> funds = brapiClient.fetchFundList(assetType);
            if (funds.isEmpty()) {
                log.warn("Fund list returned 0 results for assetType={}", assetType);
                continue;
            }

            int synced = 0;
            for (BrapiFundListResponse.FundItem item : funds) {
                if (item.symbol() == null) continue;
                try {
                    saveCurrent(item);
                    upsertTicker(item);
                    synced++;
                } catch (Exception e) {
                    log.warn("Failed to save fund {}: {}", item.symbol(), e.getMessage());
                }
            }
            log.info("Funds synced for assetType={}: {}/{}", assetType, synced, funds.size());
            totalSynced += synced;
        }
        log.info("Fund sync complete: {} funds", totalSynced);
    }

    private void saveCurrent(BrapiFundListResponse.FundItem item) {
        FundIndicatorJpaEntity entity = fundRepo.findById(item.symbol())
                .orElseGet(() -> { var e = new FundIndicatorJpaEntity(); e.setSymbol(item.symbol()); return e; });

        entity.setName(item.name());
        entity.setLegalName(item.legalName());
        entity.setCnpj(item.cnpj());
        entity.setFundType(item.assetType());
        entity.setB3Classification(item.b3Classification());
        entity.setPrice(item.price());
        entity.setPriceToNav(item.priceToNav());
        entity.setNavPerShare(item.navPerShare());
        entity.setEquity(item.equity());
        entity.setTotalAssets(item.totalAssets());
        entity.setAdminName(item.administratorName());
        entity.setAdminCnpj(item.administratorCnpj());
        if (item.totalInvestors() != null) {
            entity.setTotalInvestors(item.totalInvestors().longValue());
        }
        entity.setSyncedAt(Instant.now());
        fundRepo.save(entity);
    }

    /** Upserts a fund into the tickers table so it appears in search. */
    private void upsertTicker(BrapiFundListResponse.FundItem item) {
        TickerJpaEntity ticker = tickerRepo.findBySymbol(item.symbol())
                .orElseGet(TickerJpaEntity::new);

        String name = item.name() != null ? item.name() : item.symbol();
        String fundType = item.assetType() != null ? item.assetType().toLowerCase() : "fund";

        ticker.setSymbol(item.symbol());
        ticker.setName(name);
        ticker.setLongName(item.legalName() != null ? item.legalName() : name);
        ticker.setAssetType(fundType);
        ticker.setSubType(fundType);
        ticker.setIsActive(true);
        if (item.price() != null) {
            ticker.setLastPrice(BigDecimal.valueOf(item.price()));
        }
        ticker.setSyncedAt(Instant.now());

        tickerRepo.save(ticker);
    }
}
