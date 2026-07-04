package tech.lougon.profitly.analysis.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

/** Current market quote per stock (/api/v2/stocks/quote). */
@Entity
@Table(name = "stock_quotes")
public class StockQuoteJpaEntity {

    @Id
    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;

    @Column(name = "short_name")
    private String shortName;

    @Column(name = "long_name")
    private String longName;

    @Column(name = "currency", length = 10)
    private String currency;

    @Column(name = "price")
    private Double price;

    @Column(name = "day_high")
    private Double dayHigh;

    @Column(name = "day_low")
    private Double dayLow;

    @Column(name = "change_value")
    private Double change;

    @Column(name = "change_percent")
    private Double changePercent;

    @Column(name = "market_time")
    private Instant marketTime;

    @Column(name = "market_cap")
    private Long marketCap;

    @Column(name = "volume")
    private Long volume;

    @Column(name = "previous_close")
    private Double previousClose;

    @Column(name = "open_price")
    private Double openPrice;

    @Column(name = "fifty_two_week_low")
    private Double fiftyTwoWeekLow;

    @Column(name = "fifty_two_week_high")
    private Double fiftyTwoWeekHigh;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt;

    public StockQuoteJpaEntity() {}

    public String getSymbol() { return symbol; }
    public void setSymbol(String v) { this.symbol = v; }
    public String getShortName() { return shortName; }
    public void setShortName(String v) { this.shortName = v; }
    public String getLongName() { return longName; }
    public void setLongName(String v) { this.longName = v; }
    public String getCurrency() { return currency; }
    public void setCurrency(String v) { this.currency = v; }
    public Double getPrice() { return price; }
    public void setPrice(Double v) { this.price = v; }
    public Double getDayHigh() { return dayHigh; }
    public void setDayHigh(Double v) { this.dayHigh = v; }
    public Double getDayLow() { return dayLow; }
    public void setDayLow(Double v) { this.dayLow = v; }
    public Double getChange() { return change; }
    public void setChange(Double v) { this.change = v; }
    public Double getChangePercent() { return changePercent; }
    public void setChangePercent(Double v) { this.changePercent = v; }
    public Instant getMarketTime() { return marketTime; }
    public void setMarketTime(Instant v) { this.marketTime = v; }
    public Long getMarketCap() { return marketCap; }
    public void setMarketCap(Long v) { this.marketCap = v; }
    public Long getVolume() { return volume; }
    public void setVolume(Long v) { this.volume = v; }
    public Double getPreviousClose() { return previousClose; }
    public void setPreviousClose(Double v) { this.previousClose = v; }
    public Double getOpenPrice() { return openPrice; }
    public void setOpenPrice(Double v) { this.openPrice = v; }
    public Double getFiftyTwoWeekLow() { return fiftyTwoWeekLow; }
    public void setFiftyTwoWeekLow(Double v) { this.fiftyTwoWeekLow = v; }
    public Double getFiftyTwoWeekHigh() { return fiftyTwoWeekHigh; }
    public void setFiftyTwoWeekHigh(Double v) { this.fiftyTwoWeekHigh = v; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String v) { this.logoUrl = v; }
    public Instant getSyncedAt() { return syncedAt; }
    public void setSyncedAt(Instant v) { this.syncedAt = v; }
}
