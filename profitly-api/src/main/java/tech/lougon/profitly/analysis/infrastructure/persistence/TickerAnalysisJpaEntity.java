package tech.lougon.profitly.analysis.infrastructure.persistence;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ticker_analysis")
public class TickerAnalysisJpaEntity {

    @Id
    @Column(name = "symbol", nullable = false)
    private String symbol;

    @Column(name = "trailing_pe", precision = 20, scale = 6)
    private BigDecimal trailingPE;

    @Column(name = "price_to_book", precision = 20, scale = 6)
    private BigDecimal priceToBook;

    @Column(name = "dividend_yield", precision = 20, scale = 6)
    private BigDecimal dividendYield;

    @Column(name = "beta", precision = 20, scale = 6)
    private BigDecimal beta;

    @Column(name = "earnings_per_share", precision = 20, scale = 6)
    private BigDecimal earningsPerShare;

    @Column(name = "forward_pe", precision = 20, scale = 6)
    private BigDecimal forwardPE;

    @Column(name = "peg_ratio", precision = 20, scale = 6)
    private BigDecimal pegRatio;

    @Column(name = "enterprise_to_revenue", precision = 20, scale = 6)
    private BigDecimal enterpriseToRevenue;

    @Column(name = "enterprise_to_ebitda", precision = 20, scale = 6)
    private BigDecimal enterpriseToEbitda;

    @Column(name = "market_cap")
    private Long marketCap;

    @Column(name = "enterprise_value")
    private Long enterpriseValue;

    @Column(name = "book_value", precision = 20, scale = 6)
    private BigDecimal bookValue;

    @Column(name = "week_change_52", precision = 20, scale = 6)
    private BigDecimal weekChange52;

    @Column(name = "profit_margins", precision = 20, scale = 6)
    private BigDecimal profitMargins;

    @Column(name = "shares_outstanding")
    private Long sharesOutstanding;

    @Column(name = "float_shares")
    private Long floatShares;

    @Column(name = "last_dividend_value", precision = 20, scale = 6)
    private BigDecimal lastDividendValue;

    @Column(name = "last_dividend_date")
    private String lastDividendDate;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt;

    @Column(name = "dividends_synced_at")
    private Instant dividendsSyncedAt;

    public TickerAnalysisJpaEntity() {}

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public BigDecimal getTrailingPE() { return trailingPE; }
    public void setTrailingPE(BigDecimal v) { this.trailingPE = v; }
    public BigDecimal getPriceToBook() { return priceToBook; }
    public void setPriceToBook(BigDecimal v) { this.priceToBook = v; }
    public BigDecimal getDividendYield() { return dividendYield; }
    public void setDividendYield(BigDecimal v) { this.dividendYield = v; }
    public BigDecimal getBeta() { return beta; }
    public void setBeta(BigDecimal v) { this.beta = v; }
    public BigDecimal getEarningsPerShare() { return earningsPerShare; }
    public void setEarningsPerShare(BigDecimal v) { this.earningsPerShare = v; }
    public BigDecimal getForwardPE() { return forwardPE; }
    public void setForwardPE(BigDecimal v) { this.forwardPE = v; }
    public BigDecimal getPegRatio() { return pegRatio; }
    public void setPegRatio(BigDecimal v) { this.pegRatio = v; }
    public BigDecimal getEnterpriseToRevenue() { return enterpriseToRevenue; }
    public void setEnterpriseToRevenue(BigDecimal v) { this.enterpriseToRevenue = v; }
    public BigDecimal getEnterpriseToEbitda() { return enterpriseToEbitda; }
    public void setEnterpriseToEbitda(BigDecimal v) { this.enterpriseToEbitda = v; }
    public Long getMarketCap() { return marketCap; }
    public void setMarketCap(Long v) { this.marketCap = v; }
    public Long getEnterpriseValue() { return enterpriseValue; }
    public void setEnterpriseValue(Long v) { this.enterpriseValue = v; }
    public BigDecimal getBookValue() { return bookValue; }
    public void setBookValue(BigDecimal v) { this.bookValue = v; }
    public BigDecimal getWeekChange52() { return weekChange52; }
    public void setWeekChange52(BigDecimal v) { this.weekChange52 = v; }
    public BigDecimal getProfitMargins() { return profitMargins; }
    public void setProfitMargins(BigDecimal v) { this.profitMargins = v; }
    public Long getSharesOutstanding() { return sharesOutstanding; }
    public void setSharesOutstanding(Long v) { this.sharesOutstanding = v; }
    public Long getFloatShares() { return floatShares; }
    public void setFloatShares(Long v) { this.floatShares = v; }
    public BigDecimal getLastDividendValue() { return lastDividendValue; }
    public void setLastDividendValue(BigDecimal v) { this.lastDividendValue = v; }
    public String getLastDividendDate() { return lastDividendDate; }
    public void setLastDividendDate(String v) { this.lastDividendDate = v; }
    public Instant getSyncedAt() { return syncedAt; }
    public void setSyncedAt(Instant v) { this.syncedAt = v; }
    public Instant getDividendsSyncedAt() { return dividendsSyncedAt; }
    public void setDividendsSyncedAt(Instant v) { this.dividendsSyncedAt = v; }
}
