package tech.lougon.profitly.analysis.application.dto;

import tech.lougon.profitly.analysis.domain.model.DividendEvent;
import tech.lougon.profitly.ticker.application.dto.TickerDTO;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

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

        // Cache metadata
        Instant syncedAt
) {
    public static TickerAnalysisDTO of(TickerDTO ticker,
                                       tech.lougon.profitly.analysis.domain.model.TickerAnalysis stats,
                                       List<DividendEvent> dividends) {
        return new TickerAnalysisDTO(
                ticker.symbol(), ticker.name(), ticker.longName(),
                ticker.assetType(), ticker.subType(), ticker.sector(), ticker.logoUrl(),
                ticker.lastPrice(), ticker.changePercent(), ticker.volume(), ticker.marketCap(),
                stats.trailingPE(), stats.priceToBook(), stats.dividendYield(),
                stats.beta(), stats.earningsPerShare(), stats.forwardPE(), stats.pegRatio(),
                stats.enterpriseToRevenue(), stats.enterpriseToEbitda(),
                stats.enterpriseValue(),
                stats.bookValue(), stats.weekChange52(), stats.profitMargins(),
                stats.sharesOutstanding(), stats.lastDividendValue(), stats.lastDividendDate(),
                dividends, stats.syncedAt()
        );
    }
}
