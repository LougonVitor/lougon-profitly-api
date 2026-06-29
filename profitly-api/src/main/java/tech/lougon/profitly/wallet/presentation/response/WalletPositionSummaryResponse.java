package tech.lougon.profitly.wallet.presentation.response;

import tech.lougon.profitly.wallet.application.dto.WalletPositionSummaryDTO;

import java.math.BigDecimal;

public record WalletPositionSummaryResponse(
        String id,
        String ticker,
        String logoUrl,
        Integer quantity,
        BigDecimal averagePrice,
        BigDecimal currentPrice,
        BigDecimal totalInvested,
        BigDecimal currentValue,
        BigDecimal profitOrLoss,
        BigDecimal profitOrLossPercent
) {
    public static WalletPositionSummaryResponse from(WalletPositionSummaryDTO dto) {
        return new WalletPositionSummaryResponse(
                dto.id(),
                dto.ticker(),
                dto.logoUrl(),
                dto.quantity(),
                dto.averagePrice(),
                dto.currentPrice(),
                dto.totalInvested(),
                dto.currentValue(),
                dto.profitOrLoss(),
                dto.profitOrLossPercent()
        );
    }
}
