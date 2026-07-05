package tech.lougon.profitly.analysis.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaCryptoQuoteRepository extends JpaRepository<CryptoQuoteJpaEntity, String> {
    // brapi always returns marketCap=0 for crypto, so volume is the ranking criterion
    List<CryptoQuoteJpaEntity> findAllByOrderByVolumeDesc();
}
