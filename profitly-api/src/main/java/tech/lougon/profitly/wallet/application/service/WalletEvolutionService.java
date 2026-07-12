package tech.lougon.profitly.wallet.application.service;

import org.springframework.stereotype.Service;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaPricePointRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.PricePointJpaEntity;
import tech.lougon.profitly.wallet.domain.model.EntryType;
import tech.lougon.profitly.wallet.domain.model.PositionEntry;
import tech.lougon.profitly.wallet.domain.model.Wallet;
import tech.lougon.profitly.wallet.domain.model.WalletPosition;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Monthly wallet history computed from stored price_points: for each month end,
 * cost basis (preço médio method) and market value (net quantity × last close
 * at or before that date). Positions without price history fall back to cost.
 */
@Service
public class WalletEvolutionService {

    public record EvolutionPoint(String month, BigDecimal invested, BigDecimal marketValue) {}

    private final WalletService walletService;
    private final JpaPricePointRepository pricePointRepository;
    private final tech.lougon.profitly.analysis.infrastructure.persistence.JpaTreasuryBondHistoryRepository treasuryHistoryRepository;
    private final FixedIncomeValuationService fixedIncomeValuationService;

    public WalletEvolutionService(WalletService walletService,
                                  JpaPricePointRepository pricePointRepository,
                                  tech.lougon.profitly.analysis.infrastructure.persistence.JpaTreasuryBondHistoryRepository treasuryHistoryRepository,
                                  FixedIncomeValuationService fixedIncomeValuationService) {
        this.walletService = walletService;
        this.pricePointRepository = pricePointRepository;
        this.treasuryHistoryRepository = treasuryHistoryRepository;
        this.fixedIncomeValuationService = fixedIncomeValuationService;
    }

    public List<EvolutionPoint> evolution(String walletId, String userId) {
        Wallet wallet = walletService.requireOwned(walletId, userId);

        LocalDate firstDate = wallet.positions().stream()
                .flatMap(p -> p.entries().stream())
                .map(PositionEntry::date)
                .min(Comparator.naturalOrder())
                .orElse(null);
        if (firstDate == null) return List.of();

        YearMonth first = YearMonth.from(firstDate);
        YearMonth current = YearMonth.now();

        List<YearMonth> months = new ArrayList<>();
        for (YearMonth ym = first; !ym.isAfter(current); ym = ym.plusMonths(1)) months.add(ym);
        List<LocalDate> monthEnds = months.stream()
                .map(ym -> ym.equals(current) ? LocalDate.now() : ym.atEndOfMonth())
                .toList();

        List<PositionState> states = wallet.positions().stream()
                .map(p -> new PositionState(p, loadPrices(p, firstDate, monthEnds)))
                .toList();

        List<EvolutionPoint> points = new ArrayList<>();
        for (int i = 0; i < months.size(); i++) {
            YearMonth ym = months.get(i);
            LocalDate monthEnd = monthEnds.get(i);
            BigDecimal invested = BigDecimal.ZERO;
            BigDecimal marketValue = BigDecimal.ZERO;

            for (PositionState state : states) {
                state.advanceTo(monthEnd);
                if (state.quantity.signum() <= 0) continue;

                BigDecimal cost = state.avgPrice.multiply(state.quantity);
                invested = invested.add(cost);
                BigDecimal close = state.closeAtOrBefore(monthEnd);
                marketValue = marketValue.add(close != null
                        ? close.multiply(state.quantity)
                        : cost);
            }

            points.add(new EvolutionPoint(ym.toString(),
                    invested.setScale(2, RoundingMode.HALF_UP),
                    marketValue.setScale(2, RoundingMode.HALF_UP)));
        }
        return points;
    }

    private NavigableMap<LocalDate, BigDecimal> loadPrices(WalletPosition position, LocalDate from, List<LocalDate> sampleDates) {
        if (position.isFixedIncome()) {
            var details = position.fixedIncomeDetails();
            return fixedIncomeValuationService.factorSeries(
                    details.indexer(), details.ratePercent(), from, sampleDates, details.maturityDate());
        }

        String symbol = position.ticker();
        NavigableMap<LocalDate, BigDecimal> prices = new TreeMap<>();
        // Treasury history lives in treasury_bond_history, not price_points; the
        // sell price is the mark-to-market value of a bond already held.
        if (symbol.toLowerCase().startsWith("tesouro-")) {
            for (var h : treasuryHistoryRepository.findBySymbolOrderByReferenceDateAsc(symbol)) {
                if (h.getSellPrice() == null || h.getReferenceDate() == null
                        || h.getReferenceDate().length() < 10) continue;
                try {
                    LocalDate d = LocalDate.parse(h.getReferenceDate().substring(0, 10));
                    if (!d.isBefore(from)) prices.put(d, BigDecimal.valueOf(h.getSellPrice()));
                } catch (java.time.format.DateTimeParseException ignored) {
                    // skip malformed reference dates
                }
            }
            return prices;
        }
        for (PricePointJpaEntity p : pricePointRepository
                .findBySymbolAndDateBetweenOrderByDateAsc(symbol, from, LocalDate.now())) {
            if (p.getClose() != null) prices.put(p.getDate(), p.getClose());
        }
        return prices;
    }

    /** Walks a position's entries chronologically, keeping quantity and average cost. */
    private static final class PositionState {
        private final List<PositionEntry> ordered;
        private final NavigableMap<LocalDate, BigDecimal> prices;
        private int cursor = 0;
        private BigDecimal quantity = BigDecimal.ZERO;
        private BigDecimal avgPrice = BigDecimal.ZERO;

        PositionState(WalletPosition position, NavigableMap<LocalDate, BigDecimal> prices) {
            this.ordered = position.entries().stream()
                    .sorted(Comparator.comparing(PositionEntry::date)
                            .thenComparing(e -> e.createdAt() != null ? e.createdAt() : Instant.EPOCH))
                    .toList();
            this.prices = prices;
        }

        void advanceTo(LocalDate date) {
            while (cursor < ordered.size() && !ordered.get(cursor).date().isAfter(date)) {
                PositionEntry e = ordered.get(cursor++);
                if (e.quantity() == null || e.paidPrice() == null) continue;
                if (e.typeOrBuy() == EntryType.SELL) {
                    quantity = quantity.subtract(e.quantity());
                    if (quantity.signum() <= 0) {
                        quantity = BigDecimal.ZERO;
                        avgPrice = BigDecimal.ZERO;
                    }
                } else {
                    BigDecimal totalCost = avgPrice.multiply(quantity)
                            .add(e.paidPrice().multiply(e.quantity()));
                    quantity = quantity.add(e.quantity());
                    avgPrice = quantity.signum() == 0 ? BigDecimal.ZERO
                            : totalCost.divide(quantity, 4, RoundingMode.HALF_UP);
                }
            }
        }

        BigDecimal closeAtOrBefore(LocalDate date) {
            var floor = prices.floorEntry(date);
            return floor != null ? floor.getValue() : null;
        }
    }
}
