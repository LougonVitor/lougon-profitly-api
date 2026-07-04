package tech.lougon.profitly.analysis.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Financial statement rows (balance-sheet, income-statement, cash-flow, value-added).
 * The full brapi payload is preserved as raw JSON so no field is ever lost —
 * key values are parsed on read by the API layer.
 */
@Entity
@Table(name = "stock_statements", uniqueConstraints = @UniqueConstraint(
        name = "uk_stock_statement", columnNames = {"symbol", "statement_type", "period_type", "end_date"}))
public class StockStatementJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;

    /** balance_sheet | income_statement | cash_flow | value_added */
    @Column(name = "statement_type", nullable = false, length = 30)
    private String statementType;

    /** yearly | quarterly */
    @Column(name = "period_type", nullable = false, length = 15)
    private String periodType;

    @Column(name = "end_date", nullable = false, length = 15)
    private String endDate;

    @Column(name = "raw_json", columnDefinition = "text", nullable = false)
    private String rawJson;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt;

    public StockStatementJpaEntity() {}

    public Long getId() { return id; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String v) { this.symbol = v; }
    public String getStatementType() { return statementType; }
    public void setStatementType(String v) { this.statementType = v; }
    public String getPeriodType() { return periodType; }
    public void setPeriodType(String v) { this.periodType = v; }
    public String getEndDate() { return endDate; }
    public void setEndDate(String v) { this.endDate = v; }
    public String getRawJson() { return rawJson; }
    public void setRawJson(String v) { this.rawJson = v; }
    public Instant getSyncedAt() { return syncedAt; }
    public void setSyncedAt(Instant v) { this.syncedAt = v; }
}
