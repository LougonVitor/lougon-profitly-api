package tech.lougon.profitly.analysis.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Stock split / bonus events (stockDividends from brapi /stocks/dividends).
 * Used to convert as-paid dividend rates to the split-adjusted basis of price_points,
 * so historical DY% is computed on a consistent basis.
 */
@Entity
@Table(name = "stock_split_events", uniqueConstraints = @UniqueConstraint(
        name = "uk_stock_split", columnNames = {"symbol", "last_date_prior", "label"}))
public class StockSplitEventJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;

    /** DESDOBRAMENTO | GRUPAMENTO | BONIFICACAO */
    @Column(name = "label", length = 40)
    private String label;

    /** Multiplicative factor, e.g. 2 for a 2:1 split, 0.001 for a 1:1000 reverse split. */
    @Column(name = "factor")
    private Double factor;

    @Column(name = "complete_factor", length = 40)
    private String completeFactor;

    @Column(name = "last_date_prior", length = 30)
    private String lastDatePrior;

    @Column(name = "approved_on", length = 30)
    private String approvedOn;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt;

    public StockSplitEventJpaEntity() {}

    public Long getId() { return id; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String v) { this.symbol = v; }
    public String getLabel() { return label; }
    public void setLabel(String v) { this.label = v; }
    public Double getFactor() { return factor; }
    public void setFactor(Double v) { this.factor = v; }
    public String getCompleteFactor() { return completeFactor; }
    public void setCompleteFactor(String v) { this.completeFactor = v; }
    public String getLastDatePrior() { return lastDatePrior; }
    public void setLastDatePrior(String v) { this.lastDatePrior = v; }
    public String getApprovedOn() { return approvedOn; }
    public void setApprovedOn(String v) { this.approvedOn = v; }
    public Instant getSyncedAt() { return syncedAt; }
    public void setSyncedAt(Instant v) { this.syncedAt = v; }
}
