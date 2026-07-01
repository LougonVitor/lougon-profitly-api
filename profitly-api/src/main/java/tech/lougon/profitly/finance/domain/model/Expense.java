package tech.lougon.profitly.finance.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record Expense(
        Long id,
        String userId,
        String title,
        BigDecimal estimatedValue,
        BigDecimal realValue,
        ExpenseStatus status,
        ExpenseType type,
        Instant createdAt
) {}
