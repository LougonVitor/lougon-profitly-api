package tech.lougon.profitly.wallet.domain.model;

import java.time.Instant;
import java.util.List;

public record Wallet(
        String id,
        String name,
        String userId,
        List<WalletPosition> positions,
        Instant createdAt
) {}