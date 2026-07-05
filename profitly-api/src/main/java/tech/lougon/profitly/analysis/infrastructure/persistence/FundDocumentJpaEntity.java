package tech.lougon.profitly.analysis.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Raw JSON storage for fund documents whose shape varies per fund type:
 * profile, portfolio, fiagro_report, fiagro_portfolio, fidc_report,
 * fidc_portfolio, fip_report. One row per symbol+type+referenceDate.
 */
@Entity
@Table(name = "fund_documents",
        uniqueConstraints = @UniqueConstraint(columnNames = {"symbol", "doc_type", "reference_date"}))
public class FundDocumentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;

    @Column(name = "doc_type", nullable = false, length = 30)
    private String docType;

    @Column(name = "reference_date", nullable = false, length = 30)
    private String referenceDate;

    @Column(name = "raw_json", columnDefinition = "text", nullable = false)
    private String rawJson;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt;

    public FundDocumentJpaEntity() {}

    public Long getId() { return id; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String v) { this.symbol = v; }
    public String getDocType() { return docType; }
    public void setDocType(String v) { this.docType = v; }
    public String getReferenceDate() { return referenceDate; }
    public void setReferenceDate(String v) { this.referenceDate = v; }
    public String getRawJson() { return rawJson; }
    public void setRawJson(String v) { this.rawJson = v; }
    public Instant getSyncedAt() { return syncedAt; }
    public void setSyncedAt(Instant v) { this.syncedAt = v; }
}
