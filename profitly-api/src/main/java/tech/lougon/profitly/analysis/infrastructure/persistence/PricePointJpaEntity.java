package tech.lougon.profitly.analysis.infrastructure.persistence;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "price_points", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"symbol", "date"})
})
public class PricePointJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "price_points_seq")
    @SequenceGenerator(name = "price_points_seq", sequenceName = "price_points_id_seq", allocationSize = 50)
    private Long id;

    @Column(name = "symbol", nullable = false)
    private String symbol;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "open", precision = 20, scale = 6)
    private BigDecimal open;

    @Column(name = "high", precision = 20, scale = 6)
    private BigDecimal high;

    @Column(name = "low", precision = 20, scale = 6)
    private BigDecimal low;

    @Column(name = "close", precision = 20, scale = 6)
    private BigDecimal close;

    @Column(name = "adjusted_close", precision = 20, scale = 6)
    private BigDecimal adjustedClose;

    @Column(name = "volume")
    private Long volume;

    public PricePointJpaEntity() {}

    public Long getId() { return id; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String v) { this.symbol = v; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate v) { this.date = v; }
    public BigDecimal getOpen() { return open; }
    public void setOpen(BigDecimal v) { this.open = v; }
    public BigDecimal getHigh() { return high; }
    public void setHigh(BigDecimal v) { this.high = v; }
    public BigDecimal getLow() { return low; }
    public void setLow(BigDecimal v) { this.low = v; }
    public BigDecimal getClose() { return close; }
    public void setClose(BigDecimal v) { this.close = v; }
    public BigDecimal getAdjustedClose() { return adjustedClose; }
    public void setAdjustedClose(BigDecimal v) { this.adjustedClose = v; }
    public Long getVolume() { return volume; }
    public void setVolume(Long v) { this.volume = v; }
}
