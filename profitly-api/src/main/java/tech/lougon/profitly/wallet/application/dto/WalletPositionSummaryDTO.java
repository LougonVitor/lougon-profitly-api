package tech.lougon.profitly.wallet.application.dto;

import java.math.BigDecimal;

public record WalletPositionSummaryDTO(
        String id,
        String ticker,
        Integer quantity,
        BigDecimal averagePrice,
        BigDecimal currentPrice,
        BigDecimal totalInvested,
        BigDecimal currentValue,
        BigDecimal profitOrLoss,
        BigDecimal profitOrLossPercent
) {}
