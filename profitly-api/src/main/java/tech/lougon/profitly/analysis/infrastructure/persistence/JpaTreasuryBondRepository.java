package tech.lougon.profitly.analysis.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JpaTreasuryBondRepository extends JpaRepository<TreasuryBondJpaEntity, String> {
    List<TreasuryBondJpaEntity> findAllByOrderByBuyRateDesc();
}
