package tech.lougon.profitly.analysis.domain.repository;

import tech.lougon.profitly.analysis.domain.model.TickerAnalysis;

import java.util.Optional;

public interface TickerAnalysisRepository {
    Optional<TickerAnalysis> findBySymbol(String symbol);
    TickerAnalysis save(TickerAnalysis analysis);
}
