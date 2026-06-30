package tech.lougon.profitly.analysis.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaTickerAnalysisRepository extends JpaRepository<TickerAnalysisJpaEntity, String> {}
