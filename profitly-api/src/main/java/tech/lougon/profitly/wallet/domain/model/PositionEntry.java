package tech.lougon.profitly.wallet.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record PositionEntry(
        String id,
        String walletPositionId,
        LocalDate date,
        Integer quantity,
        BigDecimal paidPrice,
        Instant createdAt
) {}
