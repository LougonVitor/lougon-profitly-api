package tech.lougon.profitly.finance.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record Expense(
        Long id,
        String userId,
        String title,
        String description,
        BigDecimal estimatedValue,
        BigDecimal realValue,
        ExpenseStatus status,
        ExpenseType type,
        PaymentMethod paymentMethod,
        Instant createdAt,
        boolean recurring,
        Long recurringExpenseId
) {}
