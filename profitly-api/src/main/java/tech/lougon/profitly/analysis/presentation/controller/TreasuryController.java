package tech.lougon.profitly.analysis.presentation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.lougon.profitly.analysis.application.service.TreasuryAnalysisService;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaTreasuryBondRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaTreasuryBondHistoryRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.TreasuryBondJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.TreasuryBondHistoryJpaEntity;

import java.util.List;

@RestController
@RequestMapping("/api/treasury")
public class TreasuryController {

    private final JpaTreasuryBondRepository bondRepo;
    private final JpaTreasuryBondHistoryRepository historyRepo;
    private final TreasuryAnalysisService analysisService;

    public TreasuryController(JpaTreasuryBondRepository bondRepo,
                               JpaTreasuryBondHistoryRepository historyRepo,
                               TreasuryAnalysisService analysisService) {
        this.bondRepo = bondRepo;
        this.historyRepo = historyRepo;
        this.analysisService = analysisService;
    }

    @GetMapping("/bonds")
    public ResponseEntity<List<TreasuryBondJpaEntity>> getAll() {
        return ResponseEntity.ok(bondRepo.findAllByOrderByBuyRateDesc());
    }

    @GetMapping("/bonds/{symbol}")
    public ResponseEntity<?> getBond(@PathVariable String symbol) {
        return bondRepo.findById(symbol)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/bonds/{symbol}/history")
    public ResponseEntity<List<TreasuryBondHistoryJpaEntity>> getHistory(@PathVariable String symbol) {
        return ResponseEntity.ok(historyRepo.findBySymbolOrderByReferenceDateAsc(symbol));
    }

    @GetMapping("/analysis/{symbol}")
    public ResponseEntity<?> getAnalysis(@PathVariable String symbol) {
        return analysisService.getAnalysis(symbol)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
