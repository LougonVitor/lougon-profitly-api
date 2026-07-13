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
        List<WalletPositionSummaryDTO> allPositions = wallet.positions().stream()
                .map(position -> toPositionSummaryDTO(position, marketDataByTicker.get(position.ticker())))
                .toList();

        BigDecimal totalInvested = allPositions.stream()
                .map(WalletPositionSummaryDTO::totalInvested)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal currentValue = allPositions.stream()
                .map(WalletPositionSummaryDTO::currentValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal profitOrLoss = currentValue.subtract(totalInvested);

        BigDecimal profitOrLossPercent = totalInvested.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : profitOrLoss.divide(totalInvested, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));

        // realizedProfitOrLoss must include fully-sold (qty=0) positions — that's exactly where a
        // position's realized gain/loss ends up once it's closed out — so it's summed from
        // allPositions, not from the filtered list below.
        BigDecimal realized = allPositions.stream()
                .map(WalletPositionSummaryDTO::realizedProfitOrLoss)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // A fully sold/redeemed position (qty=0) has nothing left to show in the holdings table —
        // its contribution already landed in realizedProfitOrLoss above — so it's dropped here,
        // after the totals are computed from the unfiltered list.
        List<WalletPositionSummaryDTO> visiblePositions = allPositions.stream()
                .filter(p -> p.quantity().compareTo(BigDecimal.ZERO) != 0)
                .toList();

        return new WalletSummaryDTO(
                wallet.id(),
                wallet.name(),
                visiblePositions,
                totalInvested,
                currentValue,
                profitOrLoss,
                profitOrLossPercent,
                realized,
                wallet.sourceOrManual().name(),
                wallet.createdAt()
        );
    }

    private WalletPositionSummaryDTO toPositionSummaryDTO(WalletPosition position, StockMarketData marketData) {
        BigDecimal currentPrice = marketData != null ? marketData.currentPrice() : BigDecimal.ZERO;
        String logoUrl = marketData != null ? marketData.logoUrl() : null;
        String assetType = marketData != null ? marketData.assetType() : null;
        String name = marketData != null ? marketData.name() : null;

        BigDecimal averagePrice = position.averagePrice();
        BigDecimal qty = position.totalQuantity();

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

        var fi = position.fixedIncomeDetails();
        return new WalletPositionSummaryDTO(
                position.id(),
                position.ticker(),
                name,
                logoUrl,
                assetType,
                qty,
                averagePrice,
                currentPrice,
                totalInvested,
                currentValue,
                profitOrLoss,
                profitOrLossPercent,
                position.realizedProfitOrLoss(),
                entries,
                fi != null ? fi.issuer() : null,
                fi != null && fi.instrumentType() != null ? fi.instrumentType().name() : null,
                fi != null && fi.indexer() != null ? fi.indexer().name() : null,
                fi != null ? fi.ratePercent() : null,
                fi != null ? fi.dailyLiquidity() : null,
                fi != null ? fi.maturityDate() : null
        );
    }

    private PositionEntryDTO toEntryDTO(PositionEntry entry) {
        BigDecimal total = entry.paidPrice().multiply(entry.quantity());
        return new PositionEntryDTO(entry.id(), entry.date(), entry.quantity(), entry.paidPrice(),
                entry.typeOrBuy().name(), total);
    }
}
