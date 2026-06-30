package tech.lougon.profitly.ticker.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaTickerRepository extends JpaRepository<TickerJpaEntity, String> {
    Optional<TickerJpaEntity> findBySymbol(String symbol);
}
