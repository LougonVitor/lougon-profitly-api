package tech.lougon.profitly.finance.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tech.lougon.profitly.finance.domain.model.ExpenseStatus;
import tech.lougon.profitly.finance.domain.model.ExpenseType;

import java.math.BigDecimal;

public record AddExpenseRequest(
        @NotBlank String title,
        BigDecimal estimatedValue,
        BigDecimal realValue,
        ExpenseStatus status,
        @NotNull ExpenseType type
) {}
