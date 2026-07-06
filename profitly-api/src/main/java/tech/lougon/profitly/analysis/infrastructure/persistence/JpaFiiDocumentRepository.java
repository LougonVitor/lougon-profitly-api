package tech.lougon.profitly.analysis.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface JpaFiiDocumentRepository extends JpaRepository<FiiDocumentJpaEntity, Long> {
    Optional<FiiDocumentJpaEntity> findTopBySymbolAndDocTypeOrderByReferenceDateDesc(String symbol, String docType);
    Optional<FiiDocumentJpaEntity> findBySymbolAndDocTypeAndReferenceDate(String symbol, String docType, String referenceDate);
    List<FiiDocumentJpaEntity> findBySymbolAndDocTypeOrderByReferenceDateAsc(String symbol, String docType);
}
