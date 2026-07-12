package tech.lougon.profitly.wallet.presentation.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record RedeemFixedIncomeRequest(@NotNull LocalDate date) {}
