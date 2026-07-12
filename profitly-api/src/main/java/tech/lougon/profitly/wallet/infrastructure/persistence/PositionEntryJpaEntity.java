package tech.lougon.profitly.wallet.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "position_entries")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PositionEntryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_position_id", nullable = false)
    private WalletPositionJpaEntity walletPosition;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    /** Legacy integer column (NOT NULL); kept in sync with the rounded decimal quantity. */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /** Exact quantity — treasury and crypto positions are fractional. */
    @Column(name = "quantity_dec", precision = 19, scale = 8)
    private BigDecimal quantityDec;

    @Column(name = "paid_price", precision = 19, scale = 4, nullable = false)
    private BigDecimal paidPrice;

    @Column(name = "entry_type", nullable = false, length = 10, columnDefinition = "varchar(10) default 'BUY'")
    private String entryType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
