package tech.lougon.profitly.wallet.application.mapper;

import org.springframework.stereotype.Component;
import tech.lougon.profitly.wallet.application.dto.WalletPositionSummaryDTO;
import tech.lougon.profitly.wallet.application.dto.WalletSummaryDTO;
import tech.lougon.profitly.wallet.domain.model.Wallet;
import tech.lougon.profitly.wallet.domain.model.WalletPosition;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Component
public class WalletMapper {

    public WalletSummaryDTO toSummaryDTO(Wallet wallet, Map<String, BigDecimal> priceByTicker) {
        List<WalletPositionSummaryDTO> positions = wallet.positions().stream()
                .map(position -> toPositionSummaryDTO(position, priceByTicker.get(position.ticker())))
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

    private WalletPositionSummaryDTO toPositionSummaryDTO(WalletPosition position, BigDecimal currentPrice) {
        BigDecimal price = currentPrice != null ? currentPrice : BigDecimal.ZERO;
        BigDecimal qty = BigDecimal.valueOf(position.quantity());

        BigDecimal totalInvested = position.averagePrice().multiply(qty);
        BigDecimal currentValue = price.multiply(qty);
        BigDecimal profitOrLoss = currentValue.subtract(totalInvested);

        BigDecimal profitOrLossPercent = totalInvested.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : profitOrLoss.divide(totalInvested, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));

        return new WalletPositionSummaryDTO(
                position.id(),
                position.ticker(),
                position.quantity(),
                position.averagePrice(),
                price,
                totalInvested,
                currentValue,
                profitOrLoss,
                profitOrLossPercent
        );
    }
}
