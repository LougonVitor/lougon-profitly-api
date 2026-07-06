package tech.lougon.profitly.analysis.presentation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.lougon.profitly.analysis.application.service.FiiAnalysisService;
import tech.lougon.profitly.analysis.infrastructure.persistence.FiiDividendEventJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.FiiIndicatorHistoryJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.FiiIndicatorJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaFiiDividendEventRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaFiiIndicatorHistoryRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaFiiIndicatorRepository;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fii")
public class FiiIndicatorController {

    private final JpaFiiIndicatorRepository indicatorRepo;
    private final JpaFiiIndicatorHistoryRepository historyRepo;
    private final JpaFiiDividendEventRepository dividendRepo;
    private final FiiAnalysisService analysisService;

    public FiiIndicatorController(JpaFiiIndicatorRepository indicatorRepo,
                                  JpaFiiIndicatorHistoryRepository historyRepo,
                                  JpaFiiDividendEventRepository dividendRepo,
                                  FiiAnalysisService analysisService) {
        this.indicatorRepo = indicatorRepo;
        this.historyRepo = historyRepo;
        this.dividendRepo = dividendRepo;
        this.analysisService = analysisService;
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

    @GetMapping("/analysis/{symbol}")
    public ResponseEntity<?> getAnalysis(@PathVariable String symbol) {
        return analysisService.getAnalysis(symbol)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/analysis/{symbol}/dividends")
    public ResponseEntity<List<FiiDividendEventJpaEntity>> getDividends(@PathVariable String symbol) {
        return ResponseEntity.ok(
                dividendRepo.findBySymbolOrderByPaymentDateDesc(symbol.toUpperCase()));
    }
}
