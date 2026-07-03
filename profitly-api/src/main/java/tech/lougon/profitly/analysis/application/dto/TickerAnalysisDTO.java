package tech.lougon.profitly.analysis.application.dto;

import tech.lougon.profitly.analysis.domain.model.DividendEvent;
import tech.lougon.profitly.ticker.application.dto.TickerDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record TickerAnalysisDTO(
        // From Ticker entity (basic info)
        String symbol,
        String name,
        String longName,
        String assetType,
        String subType,
        String sector,
        String logoUrl,
        BigDecimal lastPrice,
        BigDecimal changePercent,
        Long volume,
        Long marketCap,

        // From TickerAnalysis (fundamentals)
        BigDecimal trailingPE,
        BigDecimal priceToBook,
        BigDecimal dividendYield,
        BigDecimal beta,
        BigDecimal earningsPerShare,
        BigDecimal forwardPE,
        BigDecimal pegRatio,
        BigDecimal enterpriseToRevenue,
        BigDecimal enterpriseToEbitda,
        Long enterpriseValue,
        BigDecimal bookValue,
        BigDecimal weekChange52,
        BigDecimal profitMargins,
        Long sharesOutstanding,
        BigDecimal lastDividendValue,
        String lastDividendDate,

        // Historical dividends
        List<DividendEvent> dividends,

        // Annual DY% computed from price_history (year → DY%). Null years have no price data.
        Map<Integer, Double> historicalDyByYear,

        // Cache metadata
        Instant syncedAt
) {
    public static TickerAnalysisDTO of(TickerDTO ticker,
                                       tech.lougon.profitly.analysis.domain.model.TickerAnalysis stats,
                                       List<DividendEvent> dividends,
                                       Map<Integer, Double> historicalDyByYear) {
        BigDecimal eps = stats.earningsPerShare();
        BigDecimal price = ticker.lastPrice();

        // P/L: use API value when present; otherwise compute price / EPS
        BigDecimal trailingPE = stats.trailingPE();
        if (trailingPE == null && eps != null && eps.compareTo(BigDecimal.ZERO) != 0 && price != null) {
            trailingPE = price.divide(eps, 2, RoundingMode.HALF_UP);
        }

        return new TickerAnalysisDTO(
                ticker.symbol(), ticker.name(), ticker.longName(),
                ticker.assetType(), ticker.subType(), ticker.sector(), ticker.logoUrl(),
                price, ticker.changePercent(), ticker.volume(), ticker.marketCap(),
                trailingPE, stats.priceToBook(), stats.dividendYield(),
                stats.beta(), eps, stats.forwardPE(), stats.pegRatio(),
                stats.enterpriseToRevenue(), stats.enterpriseToEbitda(),
                stats.enterpriseValue(),
                stats.bookValue(), stats.weekChange52(), stats.profitMargins(),
                stats.sharesOutstanding(), stats.lastDividendValue(), stats.lastDividendDate(),
                dividends, historicalDyByYear, stats.syncedAt()
        );
    }
}
