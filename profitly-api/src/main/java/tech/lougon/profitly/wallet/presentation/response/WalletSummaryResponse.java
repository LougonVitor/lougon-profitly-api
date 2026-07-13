package tech.lougon.profitly.wallet.presentation.response;

import tech.lougon.profitly.wallet.application.dto.WalletSummaryDTO;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record WalletSummaryResponse(
        String id,
        String name,
        List<WalletPositionSummaryResponse> positions,
        BigDecimal totalInvested,
        BigDecimal currentValue,
        BigDecimal profitOrLoss,
        BigDecimal profitOrLossPercent,
        BigDecimal realizedProfitOrLoss,
        String source,
        Instant createdAt
) {
    public static WalletSummaryResponse from(WalletSummaryDTO dto) {
        return new WalletSummaryResponse(
                dto.id(),
                dto.name(),
                dto.positions().stream().map(WalletPositionSummaryResponse::from).toList(),
                dto.totalInvested(),
                dto.currentValue(),
                dto.profitOrLoss(),
                dto.profitOrLossPercent(),
                dto.realizedProfitOrLoss(),
                dto.source(),
                dto.createdAt()
        );
    }
}
