package tech.lougon.profitly.analysis.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record TickerAnalysis(
        String symbol,

        // Valuation multiples
        BigDecimal trailingPE,
        BigDecimal priceToBook,
        BigDecimal dividendYield,
        BigDecimal beta,
        BigDecimal earningsPerShare,
        BigDecimal forwardPE,
        BigDecimal pegRatio,
        BigDecimal enterpriseToRevenue,
        BigDecimal enterpriseToEbitda,

        // Market data
        BigDecimal marketCap,
        BigDecimal enterpriseValue,
        BigDecimal bookValue,
        BigDecimal weekChange52,

        // Profitability
        BigDecimal profitMargins,

        // Shares
        Long sharesOutstanding,
        Long floatShares,

        // Dividends
        BigDecimal lastDividendValue,
        String lastDividendDate,

        // Cache control
        Instant syncedAt,
        Instant dividendsSyncedAt
) {}
