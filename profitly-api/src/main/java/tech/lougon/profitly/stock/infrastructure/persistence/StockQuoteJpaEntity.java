package tech.lougon.profitly.stock.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "stocks")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockQuoteJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @Column(name = "requested_symbol", nullable = false)
    private String requestedSymbol;

    @Column(name = "symbol", nullable = false, unique = true)
    private String symbol;

    @Column(name = "changed")
    private Boolean changed;

    @Column(name = "short_name")
    private String shortName;

    @Column(name = "long_name")
    private String longName;

    @Column(name = "currency", length = 10)
    private String currency;

    @Column(name = "regular_market_price", precision = 19, scale = 4)
    private BigDecimal regularMarketPrice;

    @Column(name = "regular_market_day_high", precision = 19, scale = 4)
    private BigDecimal regularMarketDayHigh;

    @Column(name = "regular_market_day_low", precision = 19, scale = 4)
    private BigDecimal regularMarketDayLow;

    @Column(name = "regular_market_day_range", length = 50)
    private String regularMarketDayRange;

    @Column(name = "regular_market_change", precision = 19, scale = 4)
    private BigDecimal regularMarketChange;

    @Column(name = "regular_market_change_percent", precision = 10, scale = 4)
    private BigDecimal regularMarketChangePercent;

    @Column(name = "regular_market_time")
    private Instant regularMarketTime;

    @Column(name = "market_cap")
    private Long marketCap;

    @Column(name = "regular_market_volume")
    private Long regularMarketVolume;

    @Column(name = "regular_market_previous_close", precision = 19, scale = 4)
    private BigDecimal regularMarketPreviousClose;

    @Column(name = "regular_market_open", precision = 19, scale = 4)
    private BigDecimal regularMarketOpen;

    @Column(name = "fifty_two_week_range", length = 50)
    private String fiftyTwoWeekRange;

    @Column(name = "fifty_two_week_low", precision = 19, scale = 4)
    private BigDecimal fiftyTwoWeekLow;

    @Column(name = "fifty_two_week_high", precision = 19, scale = 4)
    private BigDecimal fiftyTwoWeekHigh;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "asset_type", length = 20)
    private String assetType;

    @Column(name = "requested_at")
    private Instant requestedAt;

    @Column(name = "took")
    private Long took;
}