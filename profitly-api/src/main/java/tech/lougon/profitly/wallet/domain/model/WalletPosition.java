package tech.lougon.profitly.wallet.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record WalletPosition(
        String id,
        String walletId,
        String ticker,
        Integer quantity,
        BigDecimal averagePrice,
        Instant createdAt
) {}