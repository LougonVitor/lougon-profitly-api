package tech.lougon.profitly.wallet.domain.model;

/**
 * How a wallet's positions are managed. {@code MANUAL} wallets are edited by hand;
 * {@code B3} wallets were created from a B3 "Movimentação" statement and can be
 * re-imported (reintegrated) with newer statements — the import dedups so a re-run
 * never duplicates trades already recorded.
 */
public enum WalletSource {
    MANUAL,
    B3;

    public static WalletSource fromNullable(String raw) {
        if (raw == null || raw.isBlank()) return MANUAL;
        try {
            return WalletSource.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return MANUAL;
        }
    }
}
