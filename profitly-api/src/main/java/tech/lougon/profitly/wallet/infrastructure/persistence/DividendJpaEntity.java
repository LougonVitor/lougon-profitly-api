package tech.lougon.profitly.wallet.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "wallet_dividends")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DividendJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "wallet_id", nullable = false)
    private String walletId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    private String ticker;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "ex_date")
    private LocalDate exDate;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private boolean received;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
