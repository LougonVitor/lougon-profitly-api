package tech.lougon.profitly.wallet.application.mapper;

import org.springframework.stereotype.Component;
import tech.lougon.profitly.wallet.application.dto.PositionEntryDTO;
import tech.lougon.profitly.wallet.application.dto.WalletPositionSummaryDTO;
import tech.lougon.profitly.wallet.application.dto.WalletSummaryDTO;
import tech.lougon.profitly.wallet.domain.model.PositionEntry;
import tech.lougon.profitly.wallet.domain.model.Wallet;
import tech.lougon.profitly.wallet.domain.model.WalletPosition;
import tech.lougon.profitly.wallet.domain.port.StockMarketData;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Component
public class WalletMapper {

    public WalletSummaryDTO toSummaryDTO(Wallet wallet, Map<String, StockMarketData> marketDataByTicker) {
        List<WalletPositionSummaryDTO> positions = wallet.positions().stream()
                .map(position -> toPositionSummaryDTO(position, marketDataByTicker.get(position.ticker())))
                .toList();

        BigDecimal totalInvested = positions.stream()
                .map(WalletPositionSummaryDTO::totalInvested)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal currentValue = positions.stream()
                .map(WalletPositionSummaryDTO::currentValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal profitOrLoss = currentValue.subtract(totalInvested);

        BigDecimal profitOrLossPercent = totalInvested.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : profitOrLoss.divide(totalInvested, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));

        return new WalletSummaryDTO(
                wallet.id(),
                wallet.name(),
                positions,
                totalInvested,
                currentValue,
                profitOrLoss,
                profitOrLossPercent,
                wallet.createdAt()
        );
    }

    private WalletPositionSummaryDTO toPositionSummaryDTO(WalletPosition position, StockMarketData marketData) {
        BigDecimal currentPrice = marketData != null ? marketData.currentPrice() : BigDecimal.ZERO;
        String logoUrl = marketData != null ? marketData.logoUrl() : null;

        BigDecimal averagePrice = position.averagePrice();
        BigDecimal qty = BigDecimal.valueOf(position.totalQuantity());

        BigDecimal totalInvested = averagePrice.multiply(qty);
        BigDecimal currentValue = currentPrice.multiply(qty);
        BigDecimal profitOrLoss = currentValue.subtract(totalInvested);

        BigDecimal profitOrLossPercent = totalInvested.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : profitOrLoss.divide(totalInvested, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));

        List<PositionEntryDTO> entries = position.entries().stream()
                .map(this::toEntryDTO)
                .toList();

        return new WalletPositionSummaryDTO(
                position.id(),
                position.ticker(),
                logoUrl,
                position.totalQuantity(),
                averagePrice,
                currentPrice,
                totalInvested,
                currentValue,
                profitOrLoss,
                profitOrLossPercent,
                entries
        );
    }

    private PositionEntryDTO toEntryDTO(PositionEntry entry) {
        BigDecimal total = entry.paidPrice().multiply(BigDecimal.valueOf(entry.quantity()));
        return new PositionEntryDTO(entry.id(), entry.date(), entry.quantity(), entry.paidPrice(), total);
    }
}
