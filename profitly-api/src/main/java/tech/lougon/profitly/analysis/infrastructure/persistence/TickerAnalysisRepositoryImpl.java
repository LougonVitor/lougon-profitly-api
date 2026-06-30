package tech.lougon.profitly.analysis.infrastructure.persistence;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tech.lougon.profitly.analysis.domain.model.TickerAnalysis;
import tech.lougon.profitly.analysis.domain.repository.TickerAnalysisRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.mapper.AnalysisMapper;

import java.util.Optional;

@Repository
public class TickerAnalysisRepositoryImpl implements TickerAnalysisRepository {

    private final JpaTickerAnalysisRepository jpa;
    private final AnalysisMapper mapper;

    public TickerAnalysisRepositoryImpl(JpaTickerAnalysisRepository jpa, AnalysisMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public Optional<TickerAnalysis> findBySymbol(String symbol) {
        return jpa.findById(symbol).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public TickerAnalysis save(TickerAnalysis analysis) {
        TickerAnalysisJpaEntity entity = jpa.findById(analysis.symbol())
                .orElseGet(TickerAnalysisJpaEntity::new);
        mapper.applyFields(entity, analysis);
        return mapper.toDomain(jpa.save(entity));
    }
}
