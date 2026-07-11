package tech.lougon.profitly.wallet.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record PositionEntry(
        String id,
        String walletPositionId,
        LocalDate date,
        Integer quantity,
        BigDecimal paidPrice,
        EntryType type,
        Instant createdAt
) {
    public EntryType typeOrBuy() {
        return type != null ? type : EntryType.BUY;
    }

    /** Positive for buys, negative for sells. */
    public int signedQuantity() {
        return typeOrBuy() == EntryType.SELL ? -quantity : quantity;
    }
}
