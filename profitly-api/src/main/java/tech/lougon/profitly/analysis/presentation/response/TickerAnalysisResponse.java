package tech.lougon.profitly.analysis.presentation.response;

import tech.lougon.profitly.analysis.application.dto.TickerAnalysisDTO;
import tech.lougon.profitly.analysis.domain.model.DividendEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record TickerAnalysisResponse(
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
        List<DividendItem> dividends,
        Instant syncedAt
) {
    public record DividendItem(
            String assetIssued,
            String paymentDate,
            Double rate,
            String relatedTo,
            String label,
            String lastDatePrior
    ) {}

    public static TickerAnalysisResponse from(TickerAnalysisDTO dto) {
        List<DividendItem> dividendItems = dto.dividends() == null ? List.of() :
                dto.dividends().stream()
                        .map(d -> new DividendItem(
                                d.assetIssued(), d.paymentDate(), d.rate(),
                                d.relatedTo(), d.label(), d.lastDatePrior()
                        ))
                        .toList();

        return new TickerAnalysisResponse(
                dto.symbol(), dto.name(), dto.longName(), dto.assetType(), dto.subType(),
                dto.sector(), dto.logoUrl(), dto.lastPrice(), dto.changePercent(),
                dto.volume(), dto.marketCap(),
                dto.trailingPE(), dto.priceToBook(), dto.dividendYield(), dto.beta(),
                dto.earningsPerShare(), dto.forwardPE(), dto.pegRatio(),
                dto.enterpriseToRevenue(), dto.enterpriseToEbitda(),
                dto.enterpriseValue(), dto.bookValue(), dto.weekChange52(),
                dto.profitMargins(), dto.sharesOutstanding(),
                dto.lastDividendValue(), dto.lastDividendDate(),
                dividendItems, dto.syncedAt()
        );
    }
}
