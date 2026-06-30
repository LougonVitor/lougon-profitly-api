package tech.lougon.profitly.ticker.application.dto;

import java.math.BigDecimal;

public record TickerDTO(
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
) {}
