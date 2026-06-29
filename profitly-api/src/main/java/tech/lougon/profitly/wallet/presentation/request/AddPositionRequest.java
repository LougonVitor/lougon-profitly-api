package tech.lougon.profitly.wallet.presentation.request;

import java.math.BigDecimal;

public record AddPositionRequest(
        String ticker,
        Integer quantity,
        BigDecimal averagePrice
) {}
