package tech.lougon.profitly.analysis.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Advanced fund analysis (FIAGRO, FI-Infra, FIDC, FIP): current snapshot enriched
 * with indicators computed from the daily NAV series, dividend events and the raw
 * regulatory documents stored by the fund sync.
 */
public record FundAnalysisDTO(
        String symbol,
        String name,
        String legalName,
        String cnpj,
        String fundType,
        String b3Classification,
        String isin,
        String status,
        String adminName,
        String managerName,
        Double price,
        Double navPerShare,
        Double priceToNav,
        Double equity,
        Double totalAssets,
        Long totalInvestors,
        Double sharesOutstanding,
        /** Reference date of the monthly indicators reported by brapi. */
        String asOfDate,
        Instant syncedAt,
        /** Market monthly return (%) reported by brapi. */
        Double monthlyReturn,
        /** NAV monthly return (%) reported by brapi. */
        Double patrimonialMonthlyReturn,
        /** Monthly dividend yield (%) reported by brapi. */
        Double dividendYieldMonthly,
        /** Computed: dividends of the last 12 months over the current price (%). */
        Double dividendYield12m,
        /** Computed: dividends of the last month over the current price (%). */
        Double dividendYield1m,
        Double dividendsSum12m,
        Integer dividendCount12m,
        /** Market price returns (%) keyed by period: 1m, 3m, 6m, 1y, max — from price_points. */
        Map<String, Double> priceReturns,
        /** Annualized volatility (%) of daily market price log returns over the last year. */
        Double priceVolatility1y,
        /** Worst peak-to-trough market price decline (%) in the last year, negative. */
        Double priceMaxDrawdown1y,
        Double price52wHigh,
        Double price52wLow,
        /** Where the current price sits inside the 52-week range, 0–100. */
        Double pricePositionInRange52w,
        /** NAV-per-share returns (%) keyed by period: 1m, 3m, 6m, 1y, max. */
        Map<String, Double> navReturns,
        /** Net equity change (%) keyed by period: 1m, 3m, 6m, 1y, max. */
        Map<String, Double> equityChanges,
        /** Investor count change (%) keyed by period: 1m, 3m, 6m, 1y, max. */
        Map<String, Double> investorsChanges,
        Double nav52wHigh,
        Double nav52wLow,
        /** Where the current NAV sits inside the 52-week range, 0–100. */
        Double navPositionInRange52w,
        Double navHistoryHigh,
        String navHistoryHighDate,
        Double navHistoryLow,
        String navHistoryLowDate,
        /** Annualized volatility (%) of daily NAV log returns over the last year (252 sessions). */
        Double navVolatility1y,
        /** Worst peak-to-trough NAV decline (%) in the last year, negative. */
        Double maxDrawdown1y,
        /** 1-based position of this fund's computed DY 12m among funds of the same type. */
        Integer dyRankInType,
        Integer totalInType,
        Integer historyDays,
        String historyStart,
        /** Most recent dividend events, newest first. */
        List<DividendEvent> recentDividends,
        /** Other funds of the same type, ordered by DY 12m descending. */
        List<SimilarFund> similarFunds,
        /**
         * Latest raw brapi documents keyed by type: profile, portfolio, fiagro_report,
         * fiagro_portfolio, fidc_report, fidc_portfolio, fip_report. Only present types
         * appear — the frontend renders sections conditionally.
         */
        Map<String, Map<String, Object>> documents
) {
    public record DividendEvent(
            String declaredDate,
            String lastDatePrior,
            String paymentDate,
            Double rate,
            String label
    ) {}

    public record SimilarFund(
            String symbol,
            String name,
            String fundType,
            Double price,
            Double priceToNav,
            Double dividendYield12m,
            Double dividendYieldMonthly,
            Double equity,
            Long totalInvestors
    ) {}
}
