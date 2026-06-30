package tech.lougon.profitly.analysis.domain.repository;

import tech.lougon.profitly.analysis.domain.model.PricePoint;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PriceHistoryRepository {
    List<PricePoint> findBySymbolAndDateBetween(String symbol, LocalDate from, LocalDate to);
    Optional<LocalDate> findLatestDateBySymbol(String symbol);
    void saveAll(List<PricePoint> points);
}
