package tech.lougon.profitly.wallet.presentation.request;

import jakarta.validation.constraints.NotBlank;

/**
 * {@code source} is optional (defaults to MANUAL): the frontend sends "B3" when the
 * wallet is being created to hold an imported B3 statement, which unlocks the
 * reintegration flow for that wallet.
 */
public record CreateWalletRequest(@NotBlank String name, String source) {}
