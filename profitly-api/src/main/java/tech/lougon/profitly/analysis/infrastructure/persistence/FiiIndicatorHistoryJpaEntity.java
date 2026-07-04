package tech.lougon.profitly.analysis.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "fii_indicator_history",
        uniqueConstraints = @UniqueConstraint(columnNames = {"symbol", "reference_date"}))
public class FiiIndicatorHistoryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;

    @Column(name = "reference_date", nullable = false, length = 10)
    private String referenceDate;

    @Column(name = "price")
    private Double price;

    @Column(name = "nav_per_share")
    private Double navPerShare;

    @Column(name = "price_to_nav")
    private Double priceToNav;

    @Column(name = "dividend_yield_12m")
    private Double dividendYield12m;

    @Column(name = "dividend_yield_1m")
    private Double dividendYield1m;

    @Column(name = "monthly_return")
    private Double monthlyReturn;

    @Column(name = "total_investors")
    private Long totalInvestors;

    @Column(name = "shares_outstanding")
    private Long sharesOutstanding;

    @Column(name = "equity")
    private Double equity;

    @Column(name = "total_assets")
    private Double totalAssets;

    @Column(name = "segment_type", length = 30)
    private String segmentType;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt;

    public FiiIndicatorHistoryJpaEntity() {}

    public Long getId() { return id; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String v) { this.symbol = v; }
    public String getReferenceDate() { return referenceDate; }
    public void setReferenceDate(String v) { this.referenceDate = v; }
    public Double getPrice() { return price; }
    public void setPrice(Double v) { this.price = v; }
    public Double getNavPerShare() { return navPerShare; }
    public void setNavPerShare(Double v) { this.navPerShare = v; }
    public Double getPriceToNav() { return priceToNav; }
    public void setPriceToNav(Double v) { this.priceToNav = v; }
    public Double getDividendYield12m() { return dividendYield12m; }
    public void setDividendYield12m(Double v) { this.dividendYield12m = v; }
    public Double getDividendYield1m() { return dividendYield1m; }
    public void setDividendYield1m(Double v) { this.dividendYield1m = v; }
    public Double getMonthlyReturn() { return monthlyReturn; }
    public void setMonthlyReturn(Double v) { this.monthlyReturn = v; }
    public Long getTotalInvestors() { return totalInvestors; }
    public void setTotalInvestors(Long v) { this.totalInvestors = v; }
    public Long getSharesOutstanding() { return sharesOutstanding; }
    public void setSharesOutstanding(Long v) { this.sharesOutstanding = v; }
    public Double getEquity() { return equity; }
    public void setEquity(Double v) { this.equity = v; }
    public Double getTotalAssets() { return totalAssets; }
    public void setTotalAssets(Double v) { this.totalAssets = v; }
    public String getSegmentType() { return segmentType; }
    public void setSegmentType(String v) { this.segmentType = v; }
    public Instant getSyncedAt() { return syncedAt; }
    public void setSyncedAt(Instant v) { this.syncedAt = v; }
}
