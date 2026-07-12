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
        Instant createdAt,
        FixedIncomeDetails fixedIncomeDetails
) {
    public boolean isFixedIncome() {
        return fixedIncomeDetails != null;
    }

    /**
     * Rebuilds this position with a new entry list, preserving every other field —
     * in particular {@code fixedIncomeDetails}. Use this instead of the positional
     * constructor when only entries change: the constructor is easy to call with a
     * stale/incomplete field list, which would silently drop renda-fixa metadata on
     * the next save (WalletRepositoryImpl always persists the whole wallet graph).
     */
    public WalletPosition withEntries(List<PositionEntry> newEntries) {
        return new WalletPosition(id, walletId, ticker, newEntries, createdAt, fixedIncomeDetails);
    }

    public BigDecimal totalQuantity() {
        return entries.stream()
                .filter(e -> e.quantity() != null)
                .map(PositionEntry::signedQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
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
        BigDecimal qty = BigDecimal.ZERO;

        List<PositionEntry> ordered = entries.stream()
                .sorted(Comparator.comparing(PositionEntry::date)
                        .thenComparing(e -> e.createdAt() != null ? e.createdAt() : Instant.EPOCH))
                .toList();

        for (PositionEntry e : ordered) {
            if (e.quantity() == null || e.paidPrice() == null) continue;
            if (e.typeOrBuy() == EntryType.SELL) {
                realized = realized.add(e.paidPrice().subtract(avg).multiply(e.quantity()));
                qty = qty.subtract(e.quantity());
                if (qty.signum() <= 0) {
                    qty = BigDecimal.ZERO;
                    avg = BigDecimal.ZERO;
                }
            } else {
                BigDecimal totalCost = avg.multiply(qty).add(e.paidPrice().multiply(e.quantity()));
                qty = qty.add(e.quantity());
                avg = qty.signum() == 0 ? BigDecimal.ZERO
                        : totalCost.divide(qty, 4, RoundingMode.HALF_UP);
            }
        }
        return new CostWalk(avg, realized);
    }

    private record CostWalk(BigDecimal averagePrice, BigDecimal realized) {}
}
