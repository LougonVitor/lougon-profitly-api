package tech.lougon.profitly.wallet.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "wallet_positions")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WalletPositionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private WalletJpaEntity wallet;

    @Column(name = "ticker", nullable = false)
    private String ticker;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "average_price", precision = 19, scale = 4, nullable = false)
    private BigDecimal averagePrice;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
