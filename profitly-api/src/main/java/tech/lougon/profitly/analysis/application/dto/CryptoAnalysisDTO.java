package tech.lougon.profitly.analysis.application.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

/**
 * Advanced crypto analysis: current quote enriched with indicators computed
 * from the daily price history stored in price_points.
 */
public record CryptoAnalysisDTO(
        String coin,
        String coinName,
        String imageUrl,
        String currency,
        Double price,
        Double priceUsd,
        Double usdToBrlRate,
        Double changeValue,
        Double changePercent,
        Double dayHigh,
        Double dayLow,
        Double volume24h,
        Instant marketTime,
        Instant syncedAt,
        Integer volumeRank,
        Integer totalCoins,
        Double high52w,
        Double low52w,
        /** Where the current price sits inside the 52-week range, 0–100. */
        Double positionInRange52w,
        Double athPrice,
        LocalDate athDate,
        /** Negative percentage below the all-time high (0 when at ATH). */
        Double distanceFromAthPercent,
        /** Percent returns keyed by period: 7d, 1m, 3m, 6m, ytd, 1y, 2y, max. */
        Map<String, Double> returns,
        /** Annualized volatility (%) of daily log returns over the last 30 days. */
        Double volatility30d,
        /** Annualized volatility (%) of daily log returns over the last year. */
        Double volatility1y,
        /** Worst peak-to-trough decline (%) in the last year, negative. */
        Double maxDrawdown1y,
        Double sma50,
        Double sma200,
        Double priceVsSma50Percent,
        Double priceVsSma200Percent,
        Integer historyDays,
        LocalDate historyStart
) {}
