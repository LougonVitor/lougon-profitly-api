package tech.lougon.profitly.finance.domain.model;

import java.math.BigDecimal;

public record RecurringExpense(
        Long id,
        String userId,
        String title,
        BigDecimal estimatedValue,
        ExpenseType type,
        Integer dueDay,
        boolean variable
) {}
