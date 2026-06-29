package tech.lougon.profitly.wallet.domain.port;

import java.math.BigDecimal;

public record StockMarketData(
        BigDecimal currentPrice,
        String logoUrl
) {}
