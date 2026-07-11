package tech.lougon.profitly.wallet.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public record WalletPosition(
        String id,
        String walletId,
        String ticker,
        List<PositionEntry> entries,
        Instant createdAt
) {
    public int totalQuantity() {
        return entries.stream().mapToInt(PositionEntry::signedQuantity).sum();
    }

    /**
     * Average cost using the Brazilian "preço médio" method: buys update the weighted
     * average, sells reduce quantity without changing it. Entries are processed in
     * chronological order.
     */
    public BigDecimal averagePrice() {
        return costWalk().averagePrice;
    }

    /** Profit realized by sells: (sell price − average cost at the time) × quantity. */
    public BigDecimal realizedProfitOrLoss() {
        return costWalk().realized;
    }

    private CostWalk costWalk() {
        BigDecimal avg = BigDecimal.ZERO;
        BigDecimal realized = BigDecimal.ZERO;
        int qty = 0;

        List<PositionEntry> ordered = entries.stream()
                .sorted(Comparator.comparing(PositionEntry::date)
                        .thenComparing(e -> e.createdAt() != null ? e.createdAt() : Instant.EPOCH))
                .toList();

        for (PositionEntry e : ordered) {
            if (e.quantity() == null || e.paidPrice() == null) continue;
            if (e.typeOrBuy() == EntryType.SELL) {
                realized = realized.add(
                        e.paidPrice().subtract(avg).multiply(BigDecimal.valueOf(e.quantity())));
                qty -= e.quantity();
                if (qty <= 0) {
                    qty = Math.max(qty, 0);
                    if (qty == 0) avg = BigDecimal.ZERO;
                }
            } else {
                BigDecimal totalCost = avg.multiply(BigDecimal.valueOf(qty))
                        .add(e.paidPrice().multiply(BigDecimal.valueOf(e.quantity())));
                qty += e.quantity();
                avg = qty == 0 ? BigDecimal.ZERO
                        : totalCost.divide(BigDecimal.valueOf(qty), 4, RoundingMode.HALF_UP);
            }
        }
        return new CostWalk(avg, realized);
    }

    private record CostWalk(BigDecimal averagePrice, BigDecimal realized) {}
}
