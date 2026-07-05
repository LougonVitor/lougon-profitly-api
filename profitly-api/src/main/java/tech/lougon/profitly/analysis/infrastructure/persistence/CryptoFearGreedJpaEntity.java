package tech.lougon.profitly.analysis.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

/** Daily Crypto Fear & Greed Index reading (api.alternative.me). */
@Entity
@Table(name = "crypto_fear_greed")
public class CryptoFearGreedJpaEntity {

    @Id
    @Column(name = "date", nullable = false)
    private LocalDate date;

    /** 0 (extreme fear) to 100 (extreme greed). */
    @Column(name = "value", nullable = false)
    private Integer value;

    @Column(name = "classification", length = 30)
    private String classification;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt;

    public CryptoFearGreedJpaEntity() {}

    public CryptoFearGreedJpaEntity(LocalDate date, Integer value, String classification, Instant syncedAt) {
        this.date = date;
        this.value = value;
        this.classification = classification;
        this.syncedAt = syncedAt;
    }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate v) { this.date = v; }
    public Integer getValue() { return value; }
    public void setValue(Integer v) { this.value = v; }
    public String getClassification() { return classification; }
    public void setClassification(String v) { this.classification = v; }
    public Instant getSyncedAt() { return syncedAt; }
    public void setSyncedAt(Instant v) { this.syncedAt = v; }
}
