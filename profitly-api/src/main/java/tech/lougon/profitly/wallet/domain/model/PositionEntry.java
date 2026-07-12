package tech.lougon.profitly.wallet.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record PositionEntry(
        String id,
        String walletPositionId,
        LocalDate date,
        BigDecimal quantity,
        BigDecimal paidPrice,
        EntryType type,
        Instant createdAt
) {
    public EntryType typeOrBuy() {
        return type != null ? type : EntryType.BUY;
    }

    /** Positive for buys, negative for sells. */
    public BigDecimal signedQuantity() {
        return typeOrBuy() == EntryType.SELL ? quantity.negate() : quantity;
    }
}
