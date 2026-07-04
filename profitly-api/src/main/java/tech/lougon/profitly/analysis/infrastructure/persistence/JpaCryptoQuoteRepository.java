package tech.lougon.profitly.analysis.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaCryptoQuoteRepository extends JpaRepository<CryptoQuoteJpaEntity, String> {
    List<CryptoQuoteJpaEntity> findAllByOrderByMarketCapDesc();
}
