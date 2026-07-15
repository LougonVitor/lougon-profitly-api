package tech.lougon.profitly.analysis.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "fii_dividend_events",
        uniqueConstraints = @UniqueConstraint(columnNames = {"symbol", "payment_date", "rate"}))
public class FiiDividendEventJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;

    @Column(name = "approved_on", length = 30)
    private String approvedOn;

    @Column(name = "last_date_prior", length = 30)
    private String lastDatePrior;

    @Column(name = "payment_date", length = 30)
    private String paymentDate;

    @Column(name = "rate")
    private Double rate;

    @Column(name = "label", length = 60)
    private String label;

    @Column(name = "related_to", length = 30)
    private String relatedTo;

    @Column(name = "isin_code", length = 20)
    private String isinCode;

    /**
     * Which brapi endpoint the event came from: "VERTICAL" (/fii/dividends) or
     * "LEGACY" (/api/quote?dividends=true, the only source for FIIs missing from the
     * FII vertical). Legacy rates are the amounts paid at the time and are NOT
     * split-adjusted, while prices are adjusted retroactively — so any yield that
     * spans a split is meaningless for legacy rows and gets suppressed.
     */
    @Column(name = "source", length = 10, columnDefinition = "varchar(10) default 'VERTICAL'")
    private String source;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt;

    public FiiDividendEventJpaEntity() {}

    public String getSource() { return source; }
    public void setSource(String v) { this.source = v; }

    public Long getId() { return id; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String v) { this.symbol = v; }
    public String getApprovedOn() { return approvedOn; }
    public void setApprovedOn(String v) { this.approvedOn = v; }
    public String getLastDatePrior() { return lastDatePrior; }
    public void setLastDatePrior(String v) { this.lastDatePrior = v; }
    public String getPaymentDate() { return paymentDate; }
    public void setPaymentDate(String v) { this.paymentDate = v; }
    public Double getRate() { return rate; }
    public void setRate(Double v) { this.rate = v; }
    public String getLabel() { return label; }
    public void setLabel(String v) { this.label = v; }
    public String getRelatedTo() { return relatedTo; }
    public void setRelatedTo(String v) { this.relatedTo = v; }
    public String getIsinCode() { return isinCode; }
    public void setIsinCode(String v) { this.isinCode = v; }
    public Instant getSyncedAt() { return syncedAt; }
    public void setSyncedAt(Instant v) { this.syncedAt = v; }
}
