package tech.lougon.profitly.analysis.infrastructure.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tech.lougon.profitly.analysis.application.service.AnalysisService;
import tech.lougon.profitly.analysis.domain.model.DividendEvent;
import tech.lougon.profitly.analysis.domain.repository.DividendRepository;
import tech.lougon.profitly.analysis.infrastructure.client.BrapiAnalysisClient;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiDividendsResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiFinancialDataResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiStatisticsResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiStockProfileResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiStockQuoteResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiStockStatementsResponse;
import tech.lougon.profitly.analysis.infrastructure.persistence.*;
import tech.lougon.profitly.analysis.infrastructure.startup.PriceHistorySyncScheduler;
import tech.lougon.profitly.ticker.infrastructure.persistence.JpaTickerRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Full stock analysis sync from brapi — quote, profile, statistics, financial-data,
 * dividends and the four financial statements, all in batches of 20 symbols.
 * Never triggered by user requests: startup + nightly cron only.
 */
@Component
public class StockAnalysisSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(StockAnalysisSyncScheduler.class);
    private static final int BATCH_SIZE = 20;
    private static final ObjectMapper JSON = new ObjectMapper();

    private static final Map<String, String> STATEMENT_ENDPOINTS = Map.of(
            "balance-sheet", "balance_sheet",
            "income-statement", "income_statement",
            "cash-flow", "cash_flow",
            "value-added", "value_added"
    );

    private final JpaTickerRepository tickerRepo;
    private final JpaStockQuoteRepository quoteRepo;
    private final JpaStockProfileRepository profileRepo;
    private final JpaStockFinancialsRepository financialsRepo;
    private final JpaStockStatementRepository statementRepo;
    private final JpaStockSplitEventRepository splitRepo;
    private final DividendRepository dividendRepository;
    private final BrapiAnalysisClient brapiClient;
    private final AnalysisService analysisService;
    private final PriceHistorySyncScheduler priceHistoryScheduler;

    public StockAnalysisSyncScheduler(JpaTickerRepository tickerRepo,
                                      JpaStockQuoteRepository quoteRepo,
                                      JpaStockProfileRepository profileRepo,
                                      JpaStockFinancialsRepository financialsRepo,
                                      JpaStockStatementRepository statementRepo,
                                      JpaStockSplitEventRepository splitRepo,
                                      DividendRepository dividendRepository,
                                      BrapiAnalysisClient brapiClient,
                                      AnalysisService analysisService,
                                      PriceHistorySyncScheduler priceHistoryScheduler) {
        this.tickerRepo = tickerRepo;
        this.quoteRepo = quoteRepo;
        this.profileRepo = profileRepo;
        this.financialsRepo = financialsRepo;
        this.statementRepo = statementRepo;
        this.splitRepo = splitRepo;
        this.dividendRepository = dividendRepository;
        this.brapiClient = brapiClient;
        this.analysisService = analysisService;
        this.priceHistoryScheduler = priceHistoryScheduler;
    }

    @Value("${profitly.sync.on-startup:false}")
    private boolean syncOnStartup;

    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void syncOnStartup() {
        if (!syncOnStartup) return;
        log.info("Running stock analysis startup sync");
        syncAll();
    }

    /** Nightly at 20:00 BRT, after ticker/FII/fund/treasury/crypto syncs. */
    @Scheduled(cron = "0 0 20 * * *", zone = "America/Sao_Paulo")
    public void syncAll() {
        List<String> symbols = tickerRepo.findAll().stream()
                .filter(t -> "stock".equalsIgnoreCase(t.getSubType()) || "unit".equalsIgnoreCase(t.getSubType()))
                .map(t -> t.getSymbol())
                .toList();

        List<String> unitSymbols = tickerRepo.findAll().stream()
                .filter(t -> "unit".equalsIgnoreCase(t.getSubType()))
                .map(t -> t.getSymbol())
                .toList();

        if (symbols.isEmpty()) {
            log.warn("No stock/unit tickers found — skipping stock analysis sync");
            return;
        }

        log.info("Syncing stock analysis for {} tickers in batches of {}", symbols.size(), BATCH_SIZE);

        int batchIndex = 0;
        List<List<String>> batches = partition(symbols, BATCH_SIZE);
        for (List<String> batch : batches) {
            String joined = String.join(",", batch);
            batchIndex++;
            try {
                syncQuotes(joined);
                syncProfiles(joined);
                syncIndicators(joined);
                syncDividends(joined);
                for (String endpoint : STATEMENT_ENDPOINTS.keySet()) {
                    syncStatements(endpoint, joined, "annual");
                    syncStatements(endpoint, joined, "quarterly");
                }
                syncIndicatorHistory("statistics", "indicators_statistics", joined);
                syncIndicatorHistory("financial-data", "indicators_financial", joined);
            } catch (Exception e) {
                log.warn("Stock analysis batch {}/{} failed: {}", batchIndex, batches.size(), e.getMessage());
            }
            if (batchIndex % 10 == 0) {
                log.info("Stock analysis sync progress: {}/{} batches", batchIndex, batches.size());
            }
        }

        repairUnitDividends(unitSymbols);

        // Backfill price history for symbols whose dividends just arrived — without prices
        // the annual DY% chart on the analysis page stays empty (async, runs in parallel)
        priceHistoryScheduler.syncDividendTickersAsync();

        log.info("Stock analysis sync complete: {} tickers", symbols.size());
    }

    /**
     * Units (SANB11, KLBN11, TAEE11...) depend on a 3-level fallback chain that can fail
     * transiently under sync load (brapi rate limits). Retry individually — with a small
     * pause between calls — any unit that ended the sync with no dividend history at all.
     */
    private void repairUnitDividends(List<String> unitSymbols) {
        List<String> missing = unitSymbols.stream()
                .filter(s -> dividendRepository.findBySymbol(s).isEmpty())
                .toList();
        if (missing.isEmpty()) return;

        log.info("Dividend repair pass for {} units without history: {}", missing.size(), missing);
        for (String symbol : missing) {
            try {
                Thread.sleep(1500); // breathe between calls to dodge rate limits
                syncDividends(symbol);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.warn("Dividend repair failed for {}: {}", symbol, e.getMessage());
            }
        }
    }

    private void syncQuotes(String symbols) {
        for (BrapiStockQuoteResponse.Result r : brapiClient.fetchStockQuotes(symbols)) {
            try {
                StockQuoteJpaEntity e = quoteRepo.findById(r.symbol())
                        .orElseGet(() -> { var q = new StockQuoteJpaEntity(); q.setSymbol(r.symbol()); return q; });
                var d = r.data();
                e.setShortName(d.shortName());
                e.setLongName(d.longName());
                e.setCurrency(d.currency());
                e.setPrice(d.regularMarketPrice());
                e.setDayHigh(d.regularMarketDayHigh());
                e.setDayLow(d.regularMarketDayLow());
                e.setChange(d.regularMarketChange());
                e.setChangePercent(d.regularMarketChangePercent());
                e.setMarketTime(parseInstant(d.regularMarketTime()));
                e.setMarketCap(d.marketCap());
                e.setVolume(d.regularMarketVolume());
                e.setPreviousClose(d.regularMarketPreviousClose());
                e.setOpenPrice(d.regularMarketOpen());
                e.setFiftyTwoWeekLow(d.fiftyTwoWeekLow());
                e.setFiftyTwoWeekHigh(d.fiftyTwoWeekHigh());
                e.setLogoUrl(d.logoUrl());
                e.setSyncedAt(Instant.now());
                quoteRepo.save(e);
            } catch (Exception ex) {
                log.warn("Failed to save quote for {}: {}", r.symbol(), ex.getMessage());
            }
        }
    }

    private void syncProfiles(String symbols) {
        for (BrapiStockProfileResponse.Result r : brapiClient.fetchStockProfiles(symbols)) {
            try {
                StockProfileJpaEntity e = profileRepo.findById(r.symbol())
                        .orElseGet(() -> { var p = new StockProfileJpaEntity(); p.setSymbol(r.symbol()); return p; });
                var d = r.data();
                e.setName(d.name());
                e.setSector(d.sector());
                e.setSectorKey(d.sectorKey());
                e.setIndustry(d.industry());
                e.setIndustryKey(d.industryKey());
                e.setLongBusinessSummary(d.longBusinessSummary());
                e.setFullTimeEmployees(d.fullTimeEmployees());
                e.setWebsite(d.website());
                e.setTwitter(d.twitter());
                e.setStartDate(d.startDate());
                e.setCnpj(d.cnpj());
                e.setAddress1(d.address1());
                e.setAddress2(d.address2());
                e.setCity(d.city());
                e.setState(d.state());
                e.setZip(d.zip());
                e.setCountry(d.country());
                e.setPhone(d.phone());
                e.setLogoUrl(d.logoUrl());
                e.setSyncedAt(Instant.now());
                profileRepo.save(e);
            } catch (Exception ex) {
                log.warn("Failed to save profile for {}: {}", r.symbol(), ex.getMessage());
            }
        }
    }

    /** Statistics + financial-data: saved to ticker_analysis (merged) and stock_financials (full). */
    private void syncIndicators(String symbols) {
        Map<String, BrapiStatisticsResponse.Data> statsBySymbol = new HashMap<>();
        for (BrapiStatisticsResponse.Result r : brapiClient.fetchStatisticsBatch(symbols)) {
            statsBySymbol.put(r.symbol(), r.data());
        }

        Map<String, BrapiFinancialDataResponse.Data> finBySymbol = new HashMap<>();
        for (BrapiFinancialDataResponse.Result r : brapiClient.fetchFinancialDataBatch(symbols)) {
            finBySymbol.put(r.symbol(), r.data());
        }

        java.util.Set<String> all = new java.util.LinkedHashSet<>();
        all.addAll(statsBySymbol.keySet());
        all.addAll(finBySymbol.keySet());

        for (String symbol : all) {
            try {
                analysisService.saveIndicators(symbol, statsBySymbol.get(symbol), finBySymbol.get(symbol));
            } catch (Exception ex) {
                log.warn("Failed to save ticker analysis for {}: {}", symbol, ex.getMessage());
            }
            BrapiFinancialDataResponse.Data f = finBySymbol.get(symbol);
            if (f == null) continue;
            try {
                StockFinancialsJpaEntity e = financialsRepo.findById(symbol)
                        .orElseGet(() -> { var s = new StockFinancialsJpaEntity(); s.setSymbol(symbol); return s; });
                e.setTotalCash(f.totalCash());
                e.setTotalCashPerShare(dbl(f.totalCashPerShare()));
                e.setEbitda(f.ebitda());
                e.setTotalDebt(f.totalDebt());
                e.setQuickRatio(dbl(f.quickRatio()));
                e.setCurrentRatio(dbl(f.currentRatio()));
                e.setTotalRevenue(f.totalRevenue());
                e.setDebtToEquity(dbl(f.debtToEquity()));
                e.setReturnOnAssets(dbl(f.returnOnAssets()));
                e.setReturnOnEquity(dbl(f.returnOnEquity()));
                e.setGrossProfits(f.grossProfits());
                e.setFreeCashflow(f.freeCashflow());
                e.setOperatingCashflow(f.operatingCashflow());
                e.setEarningsGrowth(dbl(f.earningsGrowth()));
                e.setRevenueGrowth(dbl(f.revenueGrowth()));
                e.setEarningsGrowthAnnual(dbl(f.earningsGrowthAnnual()));
                e.setRevenueGrowthAnnual(dbl(f.revenueGrowthAnnual()));
                e.setGrossMargins(dbl(f.grossMargins()));
                e.setEbitdaMargins(dbl(f.ebitdaMargins()));
                e.setOperatingMargins(dbl(f.operatingMargins()));
                e.setProfitMargins(dbl(f.profitMargins()));
                e.setSyncedAt(Instant.now());
                financialsRepo.save(e);
            } catch (Exception ex) {
                log.warn("Failed to save financials for {}: {}", symbol, ex.getMessage());
            }
        }
    }

    private void syncDividends(String symbols) {
        for (BrapiDividendsResponse.Result r : brapiClient.fetchDividendsBatch(symbols)) {
            if (r.data().cashDividends() != null && !r.data().cashDividends().isEmpty()) {
                try {
                    List<DividendEvent> events = r.data().cashDividends().stream()
                            .map(d -> new DividendEvent(r.symbol(), d.assetIssued(), d.paymentDate(), d.rate(),
                                    d.relatedTo(), d.approvedOn(), d.label(), d.lastDatePrior(), d.remarks()))
                            .toList();
                    dividendRepository.replaceAll(r.symbol(), events);
                } catch (Exception ex) {
                    log.warn("Failed to save dividends for {}: {}", r.symbol(), ex.getMessage());
                }
            }
            saveSplits(r.symbol(), r.data().stockDividends());
        }
    }

    /** Yearly indicator history (statistics/financial-data mode=history) stored as raw rows. */
    private void syncIndicatorHistory(String endpoint, String statementType, String symbols) {
        for (BrapiStockStatementsResponse.Result r : brapiClient.fetchIndicatorHistory(endpoint, symbols)) {
            for (Map<String, Object> row : r.data()) {
                if (row == null || row.get("endDate") == null) continue;
                String endDate = String.valueOf(row.get("endDate"));
                String periodType = row.get("type") != null ? String.valueOf(row.get("type")) : "yearly";
                try {
                    StockStatementJpaEntity e = statementRepo
                            .findBySymbolAndStatementTypeAndPeriodTypeAndEndDate(r.symbol(), statementType, periodType, endDate)
                            .orElseGet(() -> {
                                var s = new StockStatementJpaEntity();
                                s.setSymbol(r.symbol());
                                s.setStatementType(statementType);
                                s.setPeriodType(periodType);
                                s.setEndDate(endDate);
                                return s;
                            });
                    e.setRawJson(JSON.writeValueAsString(row));
                    e.setSyncedAt(Instant.now());
                    statementRepo.save(e);
                } catch (Exception ex) {
                    log.warn("Failed to save {} {} for {}: {}", statementType, endDate, r.symbol(), ex.getMessage());
                }
            }
        }
    }

    /** Splits/bonuses are needed to put as-paid dividends on the same basis as adjusted prices. */
    private void saveSplits(String symbol, List<BrapiDividendsResponse.StockDividend> splits) {
        if (splits == null) return;
        for (BrapiDividendsResponse.StockDividend s : splits) {
            if (s == null || s.factor() == null || s.lastDatePrior() == null) continue;
            try {
                StockSplitEventJpaEntity e = splitRepo
                        .findBySymbolAndLastDatePriorAndLabel(symbol, s.lastDatePrior(), s.label())
                        .orElseGet(() -> {
                            var n = new StockSplitEventJpaEntity();
                            n.setSymbol(symbol);
                            n.setLastDatePrior(s.lastDatePrior());
                            n.setLabel(s.label());
                            return n;
                        });
                e.setFactor(s.factor());
                e.setCompleteFactor(s.completeFactor());
                e.setApprovedOn(s.approvedOn());
                e.setSyncedAt(Instant.now());
                splitRepo.save(e);
            } catch (Exception ex) {
                log.warn("Failed to save split event for {}: {}", symbol, ex.getMessage());
            }
        }
    }

    private void syncStatements(String endpoint, String symbols, String period) {
        String statementType = STATEMENT_ENDPOINTS.get(endpoint);
        String defaultPeriodType = "annual".equals(period) ? "yearly" : "quarterly";
        for (BrapiStockStatementsResponse.Result r : brapiClient.fetchStatements(endpoint, symbols, period)) {
            for (Map<String, Object> row : r.data()) {
                if (row == null || row.get("endDate") == null) continue;
                String endDate = String.valueOf(row.get("endDate"));
                String periodType = row.get("type") != null ? String.valueOf(row.get("type")) : defaultPeriodType;
                try {
                    StockStatementJpaEntity e = statementRepo
                            .findBySymbolAndStatementTypeAndPeriodTypeAndEndDate(r.symbol(), statementType, periodType, endDate)
                            .orElseGet(() -> {
                                var s = new StockStatementJpaEntity();
                                s.setSymbol(r.symbol());
                                s.setStatementType(statementType);
                                s.setPeriodType(periodType);
                                s.setEndDate(endDate);
                                return s;
                            });
                    e.setRawJson(JSON.writeValueAsString(row));
                    e.setSyncedAt(Instant.now());
                    statementRepo.save(e);
                } catch (Exception ex) {
                    log.warn("Failed to save {} {} for {}: {}", statementType, endDate, r.symbol(), ex.getMessage());
                }
            }
        }
    }

    private static Double dbl(java.math.BigDecimal v) {
        return v != null ? v.doubleValue() : null;
    }

    private static Instant parseInstant(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try {
            return Instant.parse(iso);
        } catch (Exception e) {
            return null;
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
