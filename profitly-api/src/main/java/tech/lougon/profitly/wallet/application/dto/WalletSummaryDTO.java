package tech.lougon.profitly.wallet.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record WalletSummaryDTO(
        String id,
        String name,
        List<WalletPositionSummaryDTO> positions,
        BigDecimal totalInvested,
        BigDecimal currentValue,
        BigDecimal profitOrLoss,
        BigDecimal profitOrLossPercent,
        BigDecimal realizedProfitOrLoss,
        String source,
        Instant createdAt
) {}
