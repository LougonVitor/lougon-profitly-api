package tech.lougon.profitly.wallet.infrastructure.persistence;

import org.springframework.stereotype.Component;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaMacroIndexValueRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.MacroIndexValueJpaEntity;
import tech.lougon.profitly.wallet.domain.port.MacroIndexLookup;

import java.time.LocalDate;
import java.util.List;

@Component
public class MacroIndexLookupImpl implements MacroIndexLookup {

    private final JpaMacroIndexValueRepository repository;

    public MacroIndexLookupImpl(JpaMacroIndexValueRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<IndexPoint> findObservations(String slug, LocalDate start, LocalDate end) {
        if (end.isBefore(start)) return List.of();
        return repository.findBySlugAndDateBetweenOrderByDateAsc(slug, start, end).stream()
                .map(e -> new IndexPoint(e.getDate(), e.getValue()))
                .toList();
    }
}
