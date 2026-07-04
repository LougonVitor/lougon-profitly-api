package tech.lougon.profitly.analysis.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

/** Catalog of coin symbols available on brapi (/api/v2/crypto/available). */
@Entity
@Table(name = "crypto_coins")
public class CryptoCoinJpaEntity {

    @Id
    @Column(name = "symbol", nullable = false, length = 30)
    private String symbol;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt;

    public CryptoCoinJpaEntity() {}

    public CryptoCoinJpaEntity(String symbol, Instant syncedAt) {
        this.symbol = symbol;
        this.syncedAt = syncedAt;
    }

    public String getSymbol() { return symbol; }
    public void setSymbol(String v) { this.symbol = v; }
    public Instant getSyncedAt() { return syncedAt; }
    public void setSyncedAt(Instant v) { this.syncedAt = v; }
}
