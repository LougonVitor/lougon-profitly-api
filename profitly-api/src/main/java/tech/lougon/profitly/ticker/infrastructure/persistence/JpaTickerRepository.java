package tech.lougon.profitly.ticker.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaTickerRepository extends JpaRepository<TickerJpaEntity, String> {
    Optional<TickerJpaEntity> findBySymbol(String symbol);
    List<TickerJpaEntity> findByAssetTypeIgnoreCase(String assetType);
}
