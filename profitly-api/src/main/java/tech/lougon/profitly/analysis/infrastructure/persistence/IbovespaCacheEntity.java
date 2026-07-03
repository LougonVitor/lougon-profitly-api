package tech.lougon.profitly.analysis.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "ibovespa_cache")
public class IbovespaCacheEntity {

    @Id
    private String range;

    @Column(nullable = false)
    private double currentPrice;

    @Column(nullable = false)
    private double changePercent;

    @Column(nullable = false)
    private double previousClose;

    @Column(nullable = false)
    private double open;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String pointsJson;

    @Column(nullable = false)
    private Instant syncedAt;

    public IbovespaCacheEntity() {}

    public IbovespaCacheEntity(String range, double currentPrice, double changePercent,
                                double previousClose, double open, String pointsJson, Instant syncedAt) {
        this.range = range;
        this.currentPrice = currentPrice;
        this.changePercent = changePercent;
        this.previousClose = previousClose;
        this.open = open;
        this.pointsJson = pointsJson;
        this.syncedAt = syncedAt;
    }

    public String getRange() { return range; }
    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double v) { this.currentPrice = v; }
    public double getChangePercent() { return changePercent; }
    public void setChangePercent(double v) { this.changePercent = v; }
    public double getPreviousClose() { return previousClose; }
    public void setPreviousClose(double v) { this.previousClose = v; }
    public double getOpen() { return open; }
    public void setOpen(double v) { this.open = v; }
    public String getPointsJson() { return pointsJson; }
    public void setPointsJson(String v) { this.pointsJson = v; }
    public Instant getSyncedAt() { return syncedAt; }
    public void setSyncedAt(Instant v) { this.syncedAt = v; }
}
