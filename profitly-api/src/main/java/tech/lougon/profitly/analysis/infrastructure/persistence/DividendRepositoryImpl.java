package tech.lougon.profitly.analysis.infrastructure.persistence;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tech.lougon.profitly.analysis.domain.model.DividendEvent;
import tech.lougon.profitly.analysis.domain.repository.DividendRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.mapper.AnalysisMapper;

import java.util.List;

@Repository
public class DividendRepositoryImpl implements DividendRepository {

    private final JpaDividendEventRepository jpa;
    private final AnalysisMapper mapper;

    public DividendRepositoryImpl(JpaDividendEventRepository jpa, AnalysisMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public List<DividendEvent> findBySymbol(String symbol) {
        return jpa.findBySymbolOrderByLastDatePriorDesc(symbol)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional
    public void saveAll(List<DividendEvent> events) {
        jpa.saveAll(events.stream().map(mapper::toEntity).toList());
    }

    @Override
    @Transactional
    public void deleteBySymbol(String symbol) {
        jpa.deleteBySymbol(symbol);
    }
}
