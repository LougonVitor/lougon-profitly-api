package tech.lougon.profitly.analysis.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "fund_nav_history",
        uniqueConstraints = @UniqueConstraint(columnNames = {"symbol", "reference_date"}))
public class FundNavHistoryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;

    @Column(name = "reference_date", nullable = false, length = 20)
    private String referenceDate;

    @Column(name = "nav_per_share")
    private Double navPerShare;

    @Column(name = "equity")
    private Double equity;

    @Column(name = "total_assets")
    private Double totalAssets;

    @Column(name = "total_investors")
    private Long totalInvestors;

    @Column(name = "daily_applications")
    private Double dailyApplications;

    @Column(name = "daily_redemptions")
    private Double dailyRedemptions;

    @Column(name = "monthly_return")
    private Double monthlyReturn;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt;

    public FundNavHistoryJpaEntity() {}

    public Long getId() { return id; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String v) { this.symbol = v; }
    public String getReferenceDate() { return referenceDate; }
    public void setReferenceDate(String v) { this.referenceDate = v; }
    public Double getNavPerShare() { return navPerShare; }
    public void setNavPerShare(Double v) { this.navPerShare = v; }
    public Double getEquity() { return equity; }
    public void setEquity(Double v) { this.equity = v; }
    public Double getTotalAssets() { return totalAssets; }
    public void setTotalAssets(Double v) { this.totalAssets = v; }
    public Long getTotalInvestors() { return totalInvestors; }
    public void setTotalInvestors(Long v) { this.totalInvestors = v; }
    public Double getDailyApplications() { return dailyApplications; }
    public void setDailyApplications(Double v) { this.dailyApplications = v; }
    public Double getDailyRedemptions() { return dailyRedemptions; }
    public void setDailyRedemptions(Double v) { this.dailyRedemptions = v; }
    public Double getMonthlyReturn() { return monthlyReturn; }
    public void setMonthlyReturn(Double v) { this.monthlyReturn = v; }
    public Instant getSyncedAt() { return syncedAt; }
    public void setSyncedAt(Instant v) { this.syncedAt = v; }
}
