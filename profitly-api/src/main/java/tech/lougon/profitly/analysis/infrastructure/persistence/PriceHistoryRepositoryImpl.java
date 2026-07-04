package tech.lougon.profitly.analysis.infrastructure.persistence;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tech.lougon.profitly.analysis.domain.model.PricePoint;
import tech.lougon.profitly.analysis.domain.repository.PriceHistoryRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.mapper.AnalysisMapper;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @Override
    public Map<Integer, Double> avgAnnualCloseBySymbol(String symbol) {
        Map<Integer, Double> result = new HashMap<>();
        for (Object[] row : jpa.avgAnnualCloseBySymbol(symbol)) {
            int year = ((Number) row[0]).intValue();
            double avg = ((Number) row[1]).doubleValue();
            result.put(year, avg);
        }
        return result;
    }

    @Override
    public Map<Integer, Double> endOfYearCloseBySymbol(String symbol) {
        Map<Integer, Double> result = new HashMap<>();
        for (Object[] row : jpa.endOfYearCloseBySymbol(symbol)) {
            result.put(((Number) row[0]).intValue(), ((Number) row[1]).doubleValue());
        }
        return result;
    }

    @Override
    public List<String> findSymbolsWithDividendsButNoPriceHistory() {
        return jpa.findSymbolsWithDividendsButNoPriceHistory();
    }
}
