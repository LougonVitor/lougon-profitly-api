package tech.lougon.profitly.wallet.domain.model;

import java.time.Instant;
import java.util.List;

public record Wallet(
        String id,
        String name,
        String userId,
        List<WalletPosition> positions,
        Instant createdAt,
        WalletSource source
) {
    public WalletSource sourceOrManual() {
        return source != null ? source : WalletSource.MANUAL;
    }

    public boolean isB3() {
        return sourceOrManual() == WalletSource.B3;
    }
}
