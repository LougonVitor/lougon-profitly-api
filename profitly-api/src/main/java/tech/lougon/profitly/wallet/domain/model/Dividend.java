package tech.lougon.profitly.wallet.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record Dividend(
        String id,
        String walletId,
        String userId,
        String ticker,
        BigDecimal totalAmount,
        LocalDate paymentDate,
        String type,
        boolean received,
        Instant createdAt
) {}
