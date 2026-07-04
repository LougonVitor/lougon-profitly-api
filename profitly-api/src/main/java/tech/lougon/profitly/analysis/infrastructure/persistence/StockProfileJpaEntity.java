package tech.lougon.profitly.analysis.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

/** Company profile per stock (/api/v2/stocks/profile). Sector feeds sector-average analysis. */
@Entity
@Table(name = "stock_profiles")
public class StockProfileJpaEntity {

    @Id
    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;

    @Column(name = "name")
    private String name;

    @Column(name = "sector", length = 80)
    private String sector;

    @Column(name = "sector_key", length = 80)
    private String sectorKey;

    @Column(name = "industry", length = 120)
    private String industry;

    @Column(name = "industry_key", length = 120)
    private String industryKey;

    @Column(name = "long_business_summary", columnDefinition = "text")
    private String longBusinessSummary;

    @Column(name = "full_time_employees")
    private Long fullTimeEmployees;

    @Column(name = "website")
    private String website;

    @Column(name = "twitter", length = 80)
    private String twitter;

    @Column(name = "start_date", length = 20)
    private String startDate;

    @Column(name = "cnpj", length = 20)
    private String cnpj;

    @Column(name = "address1")
    private String address1;

    @Column(name = "address2")
    private String address2;

    @Column(name = "city", length = 80)
    private String city;

    @Column(name = "state", length = 40)
    private String state;

    @Column(name = "zip", length = 20)
    private String zip;

    @Column(name = "country", length = 40)
    private String country;

    @Column(name = "phone", length = 40)
    private String phone;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt;

    public StockProfileJpaEntity() {}

    public String getSymbol() { return symbol; }
    public void setSymbol(String v) { this.symbol = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getSector() { return sector; }
    public void setSector(String v) { this.sector = v; }
    public String getSectorKey() { return sectorKey; }
    public void setSectorKey(String v) { this.sectorKey = v; }
    public String getIndustry() { return industry; }
    public void setIndustry(String v) { this.industry = v; }
    public String getIndustryKey() { return industryKey; }
    public void setIndustryKey(String v) { this.industryKey = v; }
    public String getLongBusinessSummary() { return longBusinessSummary; }
    public void setLongBusinessSummary(String v) { this.longBusinessSummary = v; }
    public Long getFullTimeEmployees() { return fullTimeEmployees; }
    public void setFullTimeEmployees(Long v) { this.fullTimeEmployees = v; }
    public String getWebsite() { return website; }
    public void setWebsite(String v) { this.website = v; }
    public String getTwitter() { return twitter; }
    public void setTwitter(String v) { this.twitter = v; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String v) { this.startDate = v; }
    public String getCnpj() { return cnpj; }
    public void setCnpj(String v) { this.cnpj = v; }
    public String getAddress1() { return address1; }
    public void setAddress1(String v) { this.address1 = v; }
    public String getAddress2() { return address2; }
    public void setAddress2(String v) { this.address2 = v; }
    public String getCity() { return city; }
    public void setCity(String v) { this.city = v; }
    public String getState() { return state; }
    public void setState(String v) { this.state = v; }
    public String getZip() { return zip; }
    public void setZip(String v) { this.zip = v; }
    public String getCountry() { return country; }
    public void setCountry(String v) { this.country = v; }
    public String getPhone() { return phone; }
    public void setPhone(String v) { this.phone = v; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String v) { this.logoUrl = v; }
    public Instant getSyncedAt() { return syncedAt; }
    public void setSyncedAt(Instant v) { this.syncedAt = v; }
}
