package tech.lougon.profitly.finance.presentation.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record RecurringIncomeRequest(
        @NotBlank String description,
        @NotNull @Positive BigDecimal amount,
        @Min(1) @Max(31) Integer dueDay
) {}
