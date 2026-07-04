package tech.lougon.profitly.analysis.presentation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.lougon.profitly.analysis.infrastructure.persistence.FiiIndicatorHistoryJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.FiiIndicatorJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaFiiIndicatorHistoryRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaFiiIndicatorRepository;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fii")
public class FiiIndicatorController {

    private final JpaFiiIndicatorRepository indicatorRepo;
    private final JpaFiiIndicatorHistoryRepository historyRepo;

    public FiiIndicatorController(JpaFiiIndicatorRepository indicatorRepo,
                                  JpaFiiIndicatorHistoryRepository historyRepo) {
        this.indicatorRepo = indicatorRepo;
        this.historyRepo = historyRepo;
    }

    @GetMapping("/indicators/{symbol}")
    public ResponseEntity<?> getCurrent(@PathVariable String symbol) {
        return indicatorRepo.findById(symbol.toUpperCase())
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/indicators/{symbol}/history")
    public ResponseEntity<List<FiiIndicatorHistoryJpaEntity>> getHistory(
            @PathVariable String symbol) {
        List<FiiIndicatorHistoryJpaEntity> history =
                historyRepo.findBySymbolOrderByReferenceDateAsc(symbol.toUpperCase());
        return ResponseEntity.ok(history);
    }

    /** Used by rankings: all FII indicators sorted by DY12m desc */
    @GetMapping("/indicators")
    public ResponseEntity<List<FiiIndicatorJpaEntity>> getAll() {
        return ResponseEntity.ok(indicatorRepo.findAllByOrderByDividendYield12mDesc());
    }
}
