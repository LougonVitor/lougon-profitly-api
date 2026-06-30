package tech.lougon.profitly.analysis.infrastructure.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "dividend_events", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"symbol", "asset_issued", "last_date_prior", "label"})
})
public class DividendEventJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "dividend_events_seq")
    @SequenceGenerator(name = "dividend_events_seq", sequenceName = "dividend_events_id_seq", allocationSize = 20)
    private Long id;

    @Column(name = "symbol", nullable = false)
    private String symbol;

    @Column(name = "asset_issued")
    private String assetIssued;

    @Column(name = "payment_date")
    private String paymentDate;

    @Column(name = "rate")
    private Double rate;

    @Column(name = "related_to")
    private String relatedTo;

    @Column(name = "approved_on")
    private String approvedOn;

    @Column(name = "label")
    private String label;

    @Column(name = "last_date_prior")
    private String lastDatePrior;

    @Column(name = "remarks", length = 500)
    private String remarks;

    public DividendEventJpaEntity() {}

    public Long getId() { return id; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String v) { this.symbol = v; }
    public String getAssetIssued() { return assetIssued; }
    public void setAssetIssued(String v) { this.assetIssued = v; }
    public String getPaymentDate() { return paymentDate; }
    public void setPaymentDate(String v) { this.paymentDate = v; }
    public Double getRate() { return rate; }
    public void setRate(Double v) { this.rate = v; }
    public String getRelatedTo() { return relatedTo; }
    public void setRelatedTo(String v) { this.relatedTo = v; }
    public String getApprovedOn() { return approvedOn; }
    public void setApprovedOn(String v) { this.approvedOn = v; }
    public String getLabel() { return label; }
    public void setLabel(String v) { this.label = v; }
    public String getLastDatePrior() { return lastDatePrior; }
    public void setLastDatePrior(String v) { this.lastDatePrior = v; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String v) { this.remarks = v; }
}
