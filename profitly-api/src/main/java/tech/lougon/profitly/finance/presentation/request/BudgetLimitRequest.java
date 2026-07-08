package tech.lougon.profitly.finance.presentation.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import tech.lougon.profitly.finance.domain.model.ExpenseType;

import java.math.BigDecimal;

public record BudgetLimitRequest(
        @NotNull ExpenseType type,
        @NotNull @Positive BigDecimal monthlyLimit
) {}
