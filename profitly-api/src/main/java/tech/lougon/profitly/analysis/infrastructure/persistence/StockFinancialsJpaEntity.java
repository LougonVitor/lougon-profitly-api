package tech.lougon.profitly.analysis.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

/** Current/TTM financial indicators per stock (/api/v2/stocks/financial-data). */
@Entity
@Table(name = "stock_financials")
public class StockFinancialsJpaEntity {

    @Id
    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;

    @Column(name = "total_cash")
    private Long totalCash;

    @Column(name = "total_cash_per_share")
    private Double totalCashPerShare;

    @Column(name = "ebitda")
    private Long ebitda;

    @Column(name = "total_debt")
    private Long totalDebt;

    @Column(name = "quick_ratio")
    private Double quickRatio;

    @Column(name = "current_ratio")
    private Double currentRatio;

    @Column(name = "total_revenue")
    private Long totalRevenue;

    @Column(name = "debt_to_equity")
    private Double debtToEquity;

    @Column(name = "return_on_assets")
    private Double returnOnAssets;

    @Column(name = "return_on_equity")
    private Double returnOnEquity;

    @Column(name = "gross_profits")
    private Long grossProfits;

    @Column(name = "free_cashflow")
    private Long freeCashflow;

    @Column(name = "operating_cashflow")
    private Long operatingCashflow;

    @Column(name = "earnings_growth")
    private Double earningsGrowth;

    @Column(name = "revenue_growth")
    private Double revenueGrowth;

    @Column(name = "earnings_growth_annual")
    private Double earningsGrowthAnnual;

    @Column(name = "revenue_growth_annual")
    private Double revenueGrowthAnnual;

    @Column(name = "gross_margins")
    private Double grossMargins;

    @Column(name = "ebitda_margins")
    private Double ebitdaMargins;

    @Column(name = "operating_margins")
    private Double operatingMargins;

    @Column(name = "profit_margins")
    private Double profitMargins;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt;

    public StockFinancialsJpaEntity() {}

    public String getSymbol() { return symbol; }
    public void setSymbol(String v) { this.symbol = v; }
    public Long getTotalCash() { return totalCash; }
    public void setTotalCash(Long v) { this.totalCash = v; }
    public Double getTotalCashPerShare() { return totalCashPerShare; }
    public void setTotalCashPerShare(Double v) { this.totalCashPerShare = v; }
    public Long getEbitda() { return ebitda; }
    public void setEbitda(Long v) { this.ebitda = v; }
    public Long getTotalDebt() { return totalDebt; }
    public void setTotalDebt(Long v) { this.totalDebt = v; }
    public Double getQuickRatio() { return quickRatio; }
    public void setQuickRatio(Double v) { this.quickRatio = v; }
    public Double getCurrentRatio() { return currentRatio; }
    public void setCurrentRatio(Double v) { this.currentRatio = v; }
    public Long getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(Long v) { this.totalRevenue = v; }
    public Double getDebtToEquity() { return debtToEquity; }
    public void setDebtToEquity(Double v) { this.debtToEquity = v; }
    public Double getReturnOnAssets() { return returnOnAssets; }
    public void setReturnOnAssets(Double v) { this.returnOnAssets = v; }
    public Double getReturnOnEquity() { return returnOnEquity; }
    public void setReturnOnEquity(Double v) { this.returnOnEquity = v; }
    public Long getGrossProfits() { return grossProfits; }
    public void setGrossProfits(Long v) { this.grossProfits = v; }
    public Long getFreeCashflow() { return freeCashflow; }
    public void setFreeCashflow(Long v) { this.freeCashflow = v; }
    public Long getOperatingCashflow() { return operatingCashflow; }
    public void setOperatingCashflow(Long v) { this.operatingCashflow = v; }
    public Double getEarningsGrowth() { return earningsGrowth; }
    public void setEarningsGrowth(Double v) { this.earningsGrowth = v; }
    public Double getRevenueGrowth() { return revenueGrowth; }
    public void setRevenueGrowth(Double v) { this.revenueGrowth = v; }
    public Double getEarningsGrowthAnnual() { return earningsGrowthAnnual; }
    public void setEarningsGrowthAnnual(Double v) { this.earningsGrowthAnnual = v; }
    public Double getRevenueGrowthAnnual() { return revenueGrowthAnnual; }
    public void setRevenueGrowthAnnual(Double v) { this.revenueGrowthAnnual = v; }
    public Double getGrossMargins() { return grossMargins; }
    public void setGrossMargins(Double v) { this.grossMargins = v; }
    public Double getEbitdaMargins() { return ebitdaMargins; }
    public void setEbitdaMargins(Double v) { this.ebitdaMargins = v; }
    public Double getOperatingMargins() { return operatingMargins; }
    public void setOperatingMargins(Double v) { this.operatingMargins = v; }
    public Double getProfitMargins() { return profitMargins; }
    public void setProfitMargins(Double v) { this.profitMargins = v; }
    public Instant getSyncedAt() { return syncedAt; }
    public void setSyncedAt(Instant v) { this.syncedAt = v; }
}
