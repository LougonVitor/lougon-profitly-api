package tech.lougon.profitly.analysis.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JpaFundIndicatorRepository extends JpaRepository<FundIndicatorJpaEntity, String> {
    List<FundIndicatorJpaEntity> findAllByOrderByDividendYield12mDesc();
    List<FundIndicatorJpaEntity> findByFundTypeIgnoreCase(String fundType);
}
