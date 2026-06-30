package tech.lougon.profitly.ticker.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "tickers")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TickerJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @Column(name = "symbol", nullable = false, unique = true)
    private String symbol;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "long_name")
    private String longName;

    @Column(name = "asset_type", length = 20)
    private String assetType;

    @Column(name = "sub_type", length = 30)
    private String subType;

    @Column(name = "exchange", length = 10)
    private String exchange;

    @Column(name = "currency", length = 10)
    private String currency;

    @Column(name = "sector")
    private String sector;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "last_price", precision = 19, scale = 4)
    private BigDecimal lastPrice;

    @Column(name = "change_percent", precision = 10, scale = 4)
    private BigDecimal changePercent;

    @Column(name = "volume")
    private Long volume;

    @Column(name = "market_cap")
    private Long marketCap;

    @Column(name = "synced_at")
    private Instant syncedAt;
}
