package tech.lougon.profitly.ticker.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record Ticker(
        String id,
        String symbol,
        String name,
        String longName,
        String assetType,
        String subType,
        String exchange,
        String currency,
        String sector,
        Boolean isActive,
        String logoUrl,
        BigDecimal lastPrice,
        BigDecimal changePercent,
        Long volume,
        Long marketCap,
        Instant syncedAt
) {}
