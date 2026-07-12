package tech.lougon.profitly.wallet.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AddFixedIncomeEntryRequest(
        @NotBlank String issuer,
        @NotBlank String instrumentType,
        @NotBlank String indexer,
        @NotNull @Positive BigDecimal ratePercent,
        boolean dailyLiquidity,
        @NotNull @Positive BigDecimal principal,
        @NotNull LocalDate transactionDate,
        @NotNull LocalDate maturityDate
) {}
