package tech.lougon.profitly.analysis.domain.repository;

import tech.lougon.profitly.analysis.domain.model.DividendEvent;

import java.util.List;

public interface DividendRepository {
    List<DividendEvent> findBySymbol(String symbol);
    void saveAll(List<DividendEvent> events);
    void deleteBySymbol(String symbol);
    void replaceAll(String symbol, List<DividendEvent> events);
}
