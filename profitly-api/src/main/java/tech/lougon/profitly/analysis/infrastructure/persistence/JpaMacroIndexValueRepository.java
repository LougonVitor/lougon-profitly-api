package tech.lougon.profitly.analysis.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface JpaMacroIndexValueRepository extends JpaRepository<MacroIndexValueJpaEntity, Long> {
    List<MacroIndexValueJpaEntity> findBySlugAndDateBetweenOrderByDateAsc(String slug, LocalDate start, LocalDate end);
    Optional<MacroIndexValueJpaEntity> findTopBySlugOrderByDateDesc(String slug);

    @Query("SELECT COUNT(v) FROM MacroIndexValueJpaEntity v WHERE v.slug = :slug")
    long countBySlug(@Param("slug") String slug);
}
