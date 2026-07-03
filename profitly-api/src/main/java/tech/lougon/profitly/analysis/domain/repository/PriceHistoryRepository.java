package tech.lougon.profitly.analysis.domain.repository;

import tech.lougon.profitly.analysis.domain.model.PricePoint;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import java.util.Map;

public interface PriceHistoryRepository {
    List<PricePoint> findBySymbolAndDateBetween(String symbol, LocalDate from, LocalDate to);
    Optional<LocalDate> findLatestDateBySymbol(String symbol);
    void saveAll(List<PricePoint> points);
    /** Returns year → average close price for the given symbol. */
    Map<Integer, Double> avgAnnualCloseBySymbol(String symbol);
    /** Symbols that have dividend history in DB but no price history at all. */
    List<String> findSymbolsWithDividendsButNoPriceHistory();
}
