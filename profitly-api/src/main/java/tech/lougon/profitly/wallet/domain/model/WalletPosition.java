package tech.lougon.profitly.wallet.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

public record WalletPosition(
        String id,
        String walletId,
        String ticker,
        List<PositionEntry> entries,
        Instant createdAt
) {
    public int totalQuantity() {
        return entries.stream().mapToInt(PositionEntry::quantity).sum();
    }

    public BigDecimal averagePrice() {
        int total = totalQuantity();
        if (total == 0) return BigDecimal.ZERO;
        BigDecimal weightedSum = entries.stream()
                .map(e -> e.paidPrice().multiply(BigDecimal.valueOf(e.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return weightedSum.divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP);
    }
}
