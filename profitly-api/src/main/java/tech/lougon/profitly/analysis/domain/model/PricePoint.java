package tech.lougon.profitly.analysis.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PricePoint(
        String symbol,
        LocalDate date,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        BigDecimal adjustedClose,
        Long volume
) {}
