package tech.lougon.profitly.analysis.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "treasury_bond_history",
        uniqueConstraints = @UniqueConstraint(columnNames = {"symbol", "reference_date"}))
public class TreasuryBondHistoryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "symbol", nullable = false, length = 80)
    private String symbol;

    @Column(name = "reference_date", nullable = false, length = 20)
    private String referenceDate;

    @Column(name = "buy_rate")
    private Double buyRate;

    @Column(name = "sell_rate")
    private Double sellRate;

    @Column(name = "buy_price")
    private Double buyPrice;

    @Column(name = "sell_price")
    private Double sellPrice;

    @Column(name = "base_price")
    private Double basePrice;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt;

    public TreasuryBondHistoryJpaEntity() {}

    public Long getId() { return id; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String v) { this.symbol = v; }
    public String getReferenceDate() { return referenceDate; }
    public void setReferenceDate(String v) { this.referenceDate = v; }
    public Double getBuyRate() { return buyRate; }
    public void setBuyRate(Double v) { this.buyRate = v; }
    public Double getSellRate() { return sellRate; }
    public void setSellRate(Double v) { this.sellRate = v; }
    public Double getBuyPrice() { return buyPrice; }
    public void setBuyPrice(Double v) { this.buyPrice = v; }
    public Double getSellPrice() { return sellPrice; }
    public void setSellPrice(Double v) { this.sellPrice = v; }
    public Double getBasePrice() { return basePrice; }
    public void setBasePrice(Double v) { this.basePrice = v; }
    public Instant getSyncedAt() { return syncedAt; }
    public void setSyncedAt(Instant v) { this.syncedAt = v; }
}
