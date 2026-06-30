package tech.lougon.profitly.wallet.presentation.request;

import jakarta.validation.constraints.NotBlank;

public record CreateWalletRequest(@NotBlank String name) {}
