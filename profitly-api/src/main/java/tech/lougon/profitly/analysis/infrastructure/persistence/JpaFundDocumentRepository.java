package tech.lougon.profitly.analysis.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface JpaFundDocumentRepository extends JpaRepository<FundDocumentJpaEntity, Long> {
    Optional<FundDocumentJpaEntity> findTopBySymbolAndDocTypeOrderByReferenceDateDesc(String symbol, String docType);
    Optional<FundDocumentJpaEntity> findBySymbolAndDocTypeAndReferenceDate(String symbol, String docType, String referenceDate);
    List<FundDocumentJpaEntity> findBySymbolAndDocTypeOrderByReferenceDateDesc(String symbol, String docType);
}
