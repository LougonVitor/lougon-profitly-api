package tech.lougon.profitly.analysis.application.dto;

import tech.lougon.profitly.analysis.domain.model.PricePoint;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PricePointDTO(
        LocalDate date,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        BigDecimal adjustedClose,
        Long volume
) {
    public static PricePointDTO from(PricePoint p) {
        return new PricePointDTO(p.date(), p.open(), p.high(), p.low(), p.close(), p.adjustedClose(), p.volume());
    }
}
