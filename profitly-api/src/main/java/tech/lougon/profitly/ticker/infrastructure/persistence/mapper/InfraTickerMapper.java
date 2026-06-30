package tech.lougon.profitly.ticker.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;
import tech.lougon.profitly.ticker.domain.model.Ticker;
import tech.lougon.profitly.ticker.infrastructure.persistence.TickerJpaEntity;

@Component
public class InfraTickerMapper {

    public Ticker toDomain(TickerJpaEntity e) {
        return new Ticker(
                e.getId(), e.getSymbol(), e.getName(), e.getLongName(),
                e.getAssetType(), e.getSubType(), e.getExchange(), e.getCurrency(),
                e.getSector(), e.getIsActive(), e.getLogoUrl(),
                e.getLastPrice(), e.getChangePercent(), e.getVolume(), e.getMarketCap(),
                e.getSyncedAt()
        );
    }

    public TickerJpaEntity toEntity(Ticker t) {
        TickerJpaEntity e = new TickerJpaEntity();
        applyFields(e, t);
        return e;
    }

    public void applyFields(TickerJpaEntity e, Ticker t) {
        e.setSymbol(t.symbol());
        e.setName(t.name());
        e.setLongName(t.longName());
        e.setAssetType(t.assetType());
        e.setSubType(t.subType());
        e.setExchange(t.exchange());
        e.setCurrency(t.currency());
        e.setSector(t.sector());
        e.setIsActive(t.isActive());
        e.setLogoUrl(t.logoUrl());
        e.setLastPrice(t.lastPrice());
        e.setChangePercent(t.changePercent());
        e.setVolume(t.volume());
        e.setMarketCap(t.marketCap());
        e.setSyncedAt(t.syncedAt());
    }
}
