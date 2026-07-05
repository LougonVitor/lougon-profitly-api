package tech.lougon.profitly.analysis.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

/** Current quote data per coin (/api/v2/crypto), linked to crypto_coins by symbol. */
@Entity
@Table(name = "crypto_quotes")
public class CryptoQuoteJpaEntity {

    @Id
    @Column(name = "coin", nullable = false, length = 30)
    private String coin;

    @Column(name = "coin_name")
    private String coinName;

    @Column(name = "currency", length = 10)
    private String currency;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "price")
    private Double price;

    /** BRL per USD rate used by brapi to convert the quote (currencyRateFromUSD). */
    @Column(name = "usd_to_brl_rate")
    private Double usdToBrlRate;

    @Column(name = "change_value")
    private Double changeValue;

    @Column(name = "change_percent")
    private Double changePercent;

    @Column(name = "day_high")
    private Double dayHigh;

    @Column(name = "day_low")
    private Double dayLow;

    @Column(name = "volume")
    private Double volume;

    @Column(name = "market_cap")
    private Double marketCap;

    /** Quote timestamp from brapi (ISO string parsed to Instant). New column name because
     *  the original market_time column was created as bigint and ddl-auto=update can't retype it. */
    @Column(name = "market_time_at")
    private Instant marketTime;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt;

    public CryptoQuoteJpaEntity() {}

    public String getCoin() { return coin; }
    public void setCoin(String v) { this.coin = v; }
    public String getCoinName() { return coinName; }
    public void setCoinName(String v) { this.coinName = v; }
    public String getCurrency() { return currency; }
    public void setCurrency(String v) { this.currency = v; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String v) { this.imageUrl = v; }
    public Double getPrice() { return price; }
    public void setPrice(Double v) { this.price = v; }
    public Double getUsdToBrlRate() { return usdToBrlRate; }
    public void setUsdToBrlRate(Double v) { this.usdToBrlRate = v; }
    public Double getChangeValue() { return changeValue; }
    public void setChangeValue(Double v) { this.changeValue = v; }
    public Double getChangePercent() { return changePercent; }
    public void setChangePercent(Double v) { this.changePercent = v; }
    public Double getDayHigh() { return dayHigh; }
    public void setDayHigh(Double v) { this.dayHigh = v; }
    public Double getDayLow() { return dayLow; }
    public void setDayLow(Double v) { this.dayLow = v; }
    public Double getVolume() { return volume; }
    public void setVolume(Double v) { this.volume = v; }
    public Double getMarketCap() { return marketCap; }
    public void setMarketCap(Double v) { this.marketCap = v; }
    public Instant getMarketTime() { return marketTime; }
    public void setMarketTime(Instant v) { this.marketTime = v; }
    public Instant getSyncedAt() { return syncedAt; }
    public void setSyncedAt(Instant v) { this.syncedAt = v; }
}
