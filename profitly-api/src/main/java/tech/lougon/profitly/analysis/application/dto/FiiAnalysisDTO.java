package tech.lougon.profitly.analysis.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Advanced FII analysis: current snapshot enriched with indicators computed from
 * the monthly indicator history, dividend events, market price series and the raw
 * property/portfolio documents stored by the FII sync. Everything is served from
 * the database — no brapi call.
 */
public record FiiAnalysisDTO(
        String symbol,
        String name,
        String cnpj,
        String mandate,
        String segmentoAtuacao,
        String tipoGestao,
        String segmentType,
        String adminName,
        String adminCnpj,
        Double price,
        Double navPerShare,
        Double priceToNav,
        Double equity,
        Double totalAssets,
        Long totalInvestors,
        Long sharesOutstanding,
        /** Reference date of the monthly indicators reported by brapi. */
        String asOfDate,
        Instant syncedAt,
        /** Market monthly return (%) reported by brapi. */
        Double monthlyReturn,
        Double dividendYield12m,
        Double dividendYield1m,
        Double dividendsSum12m,
        Integer dividendCount12m,
        /** Most recent payout per quota (R$). */
        Double lastDividend,
        /** Sum of dividends over the last 3 months divided by the current price (%). */
        Double dividendYield3m,
        /** Sum of dividends over the last 6 months divided by the current price (%). */
        Double dividendYield6m,
        /** Average of the monthly DY-12m values across the stored history (%). */
        Double avgDividendYield,
        /** Average daily financial volume (R$) over the last ~21 trading days. */
        Double avgDailyLiquidity,
        /** Management fee, annualized from the latest monthly report (% a.a.). */
        Double adminFeeRate,
        /** Price of one quota divided by the last monthly payout per quota — quotas needed for one "free" quota a month. */
        Double magicNumber,
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
        /** 1-based position of this FII's DY 12m among FIIs of the same segment. */
        Integer dyRankInType,
        Integer totalInType,
        Integer historyDays,
        String historyStart,
        /** Most recent dividend events, newest first. */
        List<DividendEvent> recentDividends,
        /** Other FIIs of the same segment, ordered by DY 12m descending. */
        List<SimilarFii> similarFiis,
        /** Latest area-weighted vacancy rate (%), tijolo segment only. */
        Double vacancyRate,
        /** Vacancy rate (%) by quarter reference date, oldest first — tijolo segment only. */
        Map<String, Double> vacancyHistory,
        /** Individual properties of the latest quarter, sorted by revenue share descending — tijolo segment only. */
        List<PropertyItem> properties,
        /** Portfolio composition by asset class of the latest quarter (CRI, FII, real estate, ...). */
        List<AllocationItem> portfolioAllocations,
        /** Latest raw brapi documents keyed by type: properties, portfolio. */
        Map<String, Map<String, Object>> documents
) {
    public record DividendEvent(
            String approvedOn,
            String lastDatePrior,
            String paymentDate,
            Double rate,
            String label
    ) {}

    public record SimilarFii(
            String symbol,
            String name,
            String segmentType,
            Double price,
            Double priceToNav,
            Double dividendYield12m,
            Double dividendYield1m,
            Double equity,
            Long totalInvestors
    ) {}

    public record PropertyItem(
            String name,
            String address,
            String propertyClass,
            Double area,
            Double vacancyRate,
            Double revenueShare
    ) {}

    public record AllocationItem(
            String assetClass,
            Integer count,
            Double value
    ) {}
}
