package tech.lougon.profitly.analysis.infrastructure.persistence;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "macro_index_values",
        uniqueConstraints = @UniqueConstraint(columnNames = {"slug", "date"}))
public class MacroIndexValueJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "slug", nullable = false, length = 30)
    private String slug;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "value", precision = 20, scale = 8)
    private BigDecimal value;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt;

    public MacroIndexValueJpaEntity() {}

    public Long getId() { return id; }
    public String getSlug() { return slug; }
    public void setSlug(String v) { this.slug = v; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate v) { this.date = v; }
    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal v) { this.value = v; }
    public Instant getSyncedAt() { return syncedAt; }
    public void setSyncedAt(Instant v) { this.syncedAt = v; }
}
