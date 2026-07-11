package tech.lougon.profitly.wallet.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record WalletPositionSummaryDTO(
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
        List<PositionEntryDTO> entries
) {}
