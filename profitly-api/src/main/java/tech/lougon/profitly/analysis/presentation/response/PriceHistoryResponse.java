package tech.lougon.profitly.analysis.presentation.response;

import tech.lougon.profitly.analysis.application.dto.PricePointDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PriceHistoryResponse(
        String symbol,
        String range,
        List<PriceBar> prices
) {
    public record PriceBar(
            LocalDate date,
            BigDecimal open,
            BigDecimal high,
            BigDecimal low,
            BigDecimal close,
            BigDecimal adjustedClose,
            Long volume
    ) {}

    public static PriceHistoryResponse from(String symbol, String range, List<PricePointDTO> points) {
        List<PriceBar> bars = points.stream()
                .map(p -> new PriceBar(p.date(), p.open(), p.high(), p.low(),
                        p.close(), p.adjustedClose(), p.volume()))
                .toList();
        return new PriceHistoryResponse(symbol, range, bars);
    }
}
