package tech.lougon.profitly.ticker.presentation.response;

import tech.lougon.profitly.ticker.application.dto.TickerDTO;

import java.math.BigDecimal;

public record TickerResponse(
        String symbol,
        String name,
        String longName,
        String assetType,
        String subType,
        String sector,
        Boolean isActive,
        String logoUrl,
        BigDecimal lastPrice,
        BigDecimal changePercent,
        Long volume,
        Long marketCap
) {
    public static TickerResponse from(TickerDTO dto) {
        return new TickerResponse(
                dto.symbol(), dto.name(), dto.longName(),
                dto.assetType(), dto.subType(), dto.sector(), dto.isActive(), dto.logoUrl(),
                dto.lastPrice(), dto.changePercent(), dto.volume(), dto.marketCap()
        );
    }
}
