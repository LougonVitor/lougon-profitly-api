package tech.lougon.profitly.analysis.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaCryptoCoinRepository extends JpaRepository<CryptoCoinJpaEntity, String> {
}
