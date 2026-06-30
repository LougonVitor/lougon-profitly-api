package tech.lougon.profitly.analysis.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;
import tech.lougon.profitly.analysis.domain.model.DividendEvent;
import tech.lougon.profitly.analysis.domain.model.PricePoint;
import tech.lougon.profitly.analysis.domain.model.TickerAnalysis;
import tech.lougon.profitly.analysis.infrastructure.persistence.DividendEventJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.PricePointJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.TickerAnalysisJpaEntity;

@Component
public class AnalysisMapper {

    public TickerAnalysis toDomain(TickerAnalysisJpaEntity e) {
        return new TickerAnalysis(
                e.getSymbol(), e.getTrailingPE(), e.getPriceToBook(), e.getDividendYield(),
                e.getBeta(), e.getEarningsPerShare(), e.getForwardPE(), e.getPegRatio(),
                e.getEnterpriseToRevenue(), e.getEnterpriseToEbitda(),
                e.getMarketCap(), e.getEnterpriseValue(), e.getBookValue(), e.getWeekChange52(),
                e.getProfitMargins(), e.getSharesOutstanding(), e.getFloatShares(),
                e.getLastDividendValue(), e.getLastDividendDate(),
                e.getSyncedAt(), e.getDividendsSyncedAt()
        );
    }

    public TickerAnalysisJpaEntity toEntity(TickerAnalysis d) {
        TickerAnalysisJpaEntity e = new TickerAnalysisJpaEntity();
        applyFields(e, d);
        return e;
    }

    public void applyFields(TickerAnalysisJpaEntity e, TickerAnalysis d) {
        e.setSymbol(d.symbol());
        e.setTrailingPE(d.trailingPE());
        e.setPriceToBook(d.priceToBook());
        e.setDividendYield(d.dividendYield());
        e.setBeta(d.beta());
        e.setEarningsPerShare(d.earningsPerShare());
        e.setForwardPE(d.forwardPE());
        e.setPegRatio(d.pegRatio());
        e.setEnterpriseToRevenue(d.enterpriseToRevenue());
        e.setEnterpriseToEbitda(d.enterpriseToEbitda());
        e.setMarketCap(d.marketCap());
        e.setEnterpriseValue(d.enterpriseValue());
        e.setBookValue(d.bookValue());
        e.setWeekChange52(d.weekChange52());
        e.setProfitMargins(d.profitMargins());
        e.setSharesOutstanding(d.sharesOutstanding());
        e.setFloatShares(d.floatShares());
        e.setLastDividendValue(d.lastDividendValue());
        e.setLastDividendDate(d.lastDividendDate());
        e.setSyncedAt(d.syncedAt());
        e.setDividendsSyncedAt(d.dividendsSyncedAt());
    }

    public PricePoint toDomain(PricePointJpaEntity e) {
        return new PricePoint(
                e.getSymbol(), e.getDate(), e.getOpen(), e.getHigh(),
                e.getLow(), e.getClose(), e.getAdjustedClose(), e.getVolume()
        );
    }

    public PricePointJpaEntity toEntity(PricePoint d) {
        PricePointJpaEntity e = new PricePointJpaEntity();
        e.setSymbol(d.symbol());
        e.setDate(d.date());
        e.setOpen(d.open());
        e.setHigh(d.high());
        e.setLow(d.low());
        e.setClose(d.close());
        e.setAdjustedClose(d.adjustedClose());
        e.setVolume(d.volume());
        return e;
    }

    public DividendEvent toDomain(DividendEventJpaEntity e) {
        return new DividendEvent(
                e.getSymbol(), e.getAssetIssued(), e.getPaymentDate(), e.getRate(),
                e.getRelatedTo(), e.getApprovedOn(), e.getLabel(),
                e.getLastDatePrior(), e.getRemarks()
        );
    }

    public DividendEventJpaEntity toEntity(DividendEvent d) {
        DividendEventJpaEntity e = new DividendEventJpaEntity();
        e.setSymbol(d.symbol());
        e.setAssetIssued(d.assetIssued());
        e.setPaymentDate(d.paymentDate());
        e.setRate(d.rate());
        e.setRelatedTo(d.relatedTo());
        e.setApprovedOn(d.approvedOn());
        e.setLabel(d.label());
        e.setLastDatePrior(d.lastDatePrior());
        e.setRemarks(d.remarks());
        return e;
    }
}
