package tech.lougon.profitly.analysis.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface JpaTickerAnalysisRepository extends JpaRepository<TickerAnalysisJpaEntity, String> {
    List<TickerAnalysisJpaEntity> findBySymbolIn(Collection<String> symbols);
}
