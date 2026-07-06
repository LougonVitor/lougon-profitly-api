package tech.lougon.profitly.analysis.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaFiiIndicatorRepository extends JpaRepository<FiiIndicatorJpaEntity, String> {
    List<FiiIndicatorJpaEntity> findAllByOrderByDividendYield12mDesc();
    List<FiiIndicatorJpaEntity> findBySegmentTypeIgnoreCase(String segmentType);
}
