package tech.lougon.profitly.analysis.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface JpaCryptoFearGreedRepository extends JpaRepository<CryptoFearGreedJpaEntity, LocalDate> {
    List<CryptoFearGreedJpaEntity> findByDateGreaterThanEqualOrderByDateAsc(LocalDate from);
}
