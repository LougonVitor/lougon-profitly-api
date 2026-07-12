package tech.lougon.profitly.wallet.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PositionEntryDTO(
        String id,
        LocalDate date,
        BigDecimal quantity,
        BigDecimal paidPrice,
        String type,
        BigDecimal total
) {}
