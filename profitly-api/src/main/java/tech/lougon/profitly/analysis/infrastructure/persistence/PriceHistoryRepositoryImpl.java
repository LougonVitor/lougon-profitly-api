package tech.lougon.profitly.analysis.infrastructure.persistence;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tech.lougon.profitly.analysis.domain.model.PricePoint;
import tech.lougon.profitly.analysis.domain.repository.PriceHistoryRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.mapper.AnalysisMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class PriceHistoryRepositoryImpl implements PriceHistoryRepository {

    private final JpaPricePointRepository jpa;
    private final AnalysisMapper mapper;

    public PriceHistoryRepositoryImpl(JpaPricePointRepository jpa, AnalysisMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public List<PricePoint> findBySymbolAndDateBetween(String symbol, LocalDate from, LocalDate to) {
        return jpa.findBySymbolAndDateBetweenOrderByDateAsc(symbol, from, to)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<LocalDate> findLatestDateBySymbol(String symbol) {
        return jpa.findLatestDateBySymbol(symbol);
    }

    @Override
    @Transactional
    public void saveAll(List<PricePoint> points) {
        List<PricePointJpaEntity> entities = points.stream().map(mapper::toEntity).toList();
        jpa.saveAll(entities);
    }
}
