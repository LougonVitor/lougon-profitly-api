package tech.lougon.profitly.analysis.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "fund_indicators")
public class FundIndicatorJpaEntity {

    @Id
    @Column(name = "symbol", nullable = false)
    private String symbol;

    @Column(name = "name")
    private String name;

    @Column(name = "legal_name")
    private String legalName;

    @Column(name = "cnpj", length = 20)
    private String cnpj;

    @Column(name = "fund_type", length = 30)
    private String fundType;

    @Column(name = "b3_classification", length = 100)
    private String b3Classification;

    @Column(name = "equity")
    private Double equity;

    @Column(name = "total_assets")
    private Double totalAssets;

    @Column(name = "price")
    private Double price;

    @Column(name = "dividend_yield_12m")
    private Double dividendYield12m;

    @Column(name = "dividend_yield_1m")
    private Double dividendYield1m;

    @Column(name = "price_to_nav")
    private Double priceToNav;

    @Column(name = "nav_per_share")
    private Double navPerShare;

    @Column(name = "total_investors")
    private Long totalInvestors;

    @Column(name = "admin_name")
    private String adminName;

    @Column(name = "admin_cnpj", length = 20)
    private String adminCnpj;

    @Column(name = "segment_type", length = 30)
    private String segmentType;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt;

    public FundIndicatorJpaEntity() {}

    public String getSymbol() { return symbol; }
    public void setSymbol(String v) { this.symbol = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getLegalName() { return legalName; }
    public void setLegalName(String v) { this.legalName = v; }
    public String getCnpj() { return cnpj; }
    public void setCnpj(String v) { this.cnpj = v; }
    public String getFundType() { return fundType; }
    public void setFundType(String v) { this.fundType = v; }
    public String getB3Classification() { return b3Classification; }
    public void setB3Classification(String v) { this.b3Classification = v; }
    public Double getEquity() { return equity; }
    public void setEquity(Double v) { this.equity = v; }
    public Double getTotalAssets() { return totalAssets; }
    public void setTotalAssets(Double v) { this.totalAssets = v; }
    public Double getPrice() { return price; }
    public void setPrice(Double v) { this.price = v; }
    public Double getDividendYield12m() { return dividendYield12m; }
    public void setDividendYield12m(Double v) { this.dividendYield12m = v; }
    public Double getDividendYield1m() { return dividendYield1m; }
    public void setDividendYield1m(Double v) { this.dividendYield1m = v; }
    public Double getPriceToNav() { return priceToNav; }
    public void setPriceToNav(Double v) { this.priceToNav = v; }
    public Double getNavPerShare() { return navPerShare; }
    public void setNavPerShare(Double v) { this.navPerShare = v; }
    public Long getTotalInvestors() { return totalInvestors; }
    public void setTotalInvestors(Long v) { this.totalInvestors = v; }
    public String getAdminName() { return adminName; }
    public void setAdminName(String v) { this.adminName = v; }
    public String getAdminCnpj() { return adminCnpj; }
    public void setAdminCnpj(String v) { this.adminCnpj = v; }
    public String getSegmentType() { return segmentType; }
    public void setSegmentType(String v) { this.segmentType = v; }
    public Instant getSyncedAt() { return syncedAt; }
    public void setSyncedAt(Instant v) { this.syncedAt = v; }
}
