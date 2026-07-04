package tech.lougon.profitly.analysis.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "treasury_bonds")
public class TreasuryBondJpaEntity {

    @Id
    @Column(name = "symbol", nullable = false, length = 80)
    private String symbol;

    @Column(name = "name", length = 120)
    private String name;

    @Column(name = "bond_type", length = 40)
    private String bondType;

    @Column(name = "indexer", length = 20)
    private String indexer;

    @Column(name = "coupon_type", length = 20)
    private String couponType;

    @Column(name = "maturity_date", length = 20)
    private String maturityDate;

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

    @Column(name = "duration_days")
    private Integer durationDays;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt;

    public TreasuryBondJpaEntity() {}

    public String getSymbol() { return symbol; }
    public void setSymbol(String v) { this.symbol = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getBondType() { return bondType; }
    public void setBondType(String v) { this.bondType = v; }
    public String getIndexer() { return indexer; }
    public void setIndexer(String v) { this.indexer = v; }
    public String getCouponType() { return couponType; }
    public void setCouponType(String v) { this.couponType = v; }
    public String getMaturityDate() { return maturityDate; }
    public void setMaturityDate(String v) { this.maturityDate = v; }
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
    public Integer getDurationDays() { return durationDays; }
    public void setDurationDays(Integer v) { this.durationDays = v; }
    public Instant getSyncedAt() { return syncedAt; }
    public void setSyncedAt(Instant v) { this.syncedAt = v; }
}
