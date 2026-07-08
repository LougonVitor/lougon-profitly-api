package tech.lougon.profitly.finance.domain.model;

import java.math.BigDecimal;

/** A user-defined monthly spending cap for a single expense category. */
public record BudgetLimit(
        Long id,
        String userId,
        ExpenseType type,
        BigDecimal monthlyLimit
) {}
