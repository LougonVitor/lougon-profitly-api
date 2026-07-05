package tech.lougon.profitly.analysis.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "fund_dividend_events",
        uniqueConstraints = @UniqueConstraint(columnNames = {"symbol", "payment_date", "rate"}))
public class FundDividendEventJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;

    @Column(name = "declared_date", length = 30)
    private String declaredDate;

    @Column(name = "last_date_prior", length = 30)
    private String lastDatePrior;

    @Column(name = "payment_date", length = 30)
    private String paymentDate;

    @Column(name = "rate")
    private Double rate;

    @Column(name = "label", length = 60)
    private String label;

    @Column(name = "isin_code", length = 20)
    private String isinCode;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt;

    public FundDividendEventJpaEntity() {}

    public Long getId() { return id; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String v) { this.symbol = v; }
    public String getDeclaredDate() { return declaredDate; }
    public void setDeclaredDate(String v) { this.declaredDate = v; }
    public String getLastDatePrior() { return lastDatePrior; }
    public void setLastDatePrior(String v) { this.lastDatePrior = v; }
    public String getPaymentDate() { return paymentDate; }
    public void setPaymentDate(String v) { this.paymentDate = v; }
    public Double getRate() { return rate; }
    public void setRate(Double v) { this.rate = v; }
    public String getLabel() { return label; }
    public void setLabel(String v) { this.label = v; }
    public String getIsinCode() { return isinCode; }
    public void setIsinCode(String v) { this.isinCode = v; }
    public Instant getSyncedAt() { return syncedAt; }
    public void setSyncedAt(Instant v) { this.syncedAt = v; }
}
