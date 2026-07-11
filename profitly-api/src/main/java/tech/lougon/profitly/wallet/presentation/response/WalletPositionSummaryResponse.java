package tech.lougon.profitly.wallet.presentation.response;

import tech.lougon.profitly.wallet.application.dto.PositionEntryDTO;
import tech.lougon.profitly.wallet.application.dto.WalletPositionSummaryDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record WalletPositionSummaryResponse(
        String id,
        String ticker,
        String logoUrl,
        String assetType,
        Integer quantity,
        BigDecimal averagePrice,
        BigDecimal currentPrice,
        BigDecimal totalInvested,
        BigDecimal currentValue,
        BigDecimal profitOrLoss,
        BigDecimal profitOrLossPercent,
        BigDecimal realizedProfitOrLoss,
        List<PositionEntryResponse> entries
) {
    public static WalletPositionSummaryResponse from(WalletPositionSummaryDTO dto) {
        List<PositionEntryResponse> entries = dto.entries().stream()
                .map(PositionEntryResponse::from)
                .toList();
        return new WalletPositionSummaryResponse(
                dto.id(),
                dto.ticker(),
                dto.logoUrl(),
                dto.assetType(),
                dto.quantity(),
                dto.averagePrice(),
                dto.currentPrice(),
                dto.totalInvested(),
                dto.currentValue(),
                dto.profitOrLoss(),
                dto.profitOrLossPercent(),
                dto.realizedProfitOrLoss(),
                entries
        );
    }

    public record PositionEntryResponse(
            String id,
            LocalDate date,
            Integer quantity,
            BigDecimal paidPrice,
            String type,
            BigDecimal total
    ) {
        public static PositionEntryResponse from(PositionEntryDTO dto) {
            return new PositionEntryResponse(dto.id(), dto.date(), dto.quantity(), dto.paidPrice(), dto.type(), dto.total());
        }
    }
}
