package tech.lougon.profitly.finance.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tech.lougon.profitly.finance.domain.model.ExpenseType;

import java.math.BigDecimal;

public record RecurringExpenseRequest(
        @NotBlank String title,
        BigDecimal estimatedValue,
        @NotNull ExpenseType type
) {}
