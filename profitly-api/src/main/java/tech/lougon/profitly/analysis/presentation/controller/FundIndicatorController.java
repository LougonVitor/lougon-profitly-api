package tech.lougon.profitly.analysis.presentation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.lougon.profitly.analysis.application.service.FundAnalysisService;
import tech.lougon.profitly.analysis.infrastructure.persistence.FundDividendEventJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.FundIndicatorJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.FundNavHistoryJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaFundDividendEventRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaFundIndicatorRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaFundNavHistoryRepository;

import java.util.List;

@RestController
@RequestMapping("/api/funds")
public class FundIndicatorController {

    private final JpaFundIndicatorRepository fundRepo;
    private final JpaFundNavHistoryRepository navHistoryRepo;
    private final JpaFundDividendEventRepository dividendRepo;
    private final FundAnalysisService analysisService;

    public FundIndicatorController(JpaFundIndicatorRepository fundRepo,
                                   JpaFundNavHistoryRepository navHistoryRepo,
                                   JpaFundDividendEventRepository dividendRepo,
                                   FundAnalysisService analysisService) {
        this.fundRepo = fundRepo;
        this.navHistoryRepo = navHistoryRepo;
        this.dividendRepo = dividendRepo;
        this.analysisService = analysisService;
    }

    @GetMapping("/indicators")
    public ResponseEntity<List<FundIndicatorJpaEntity>> getAll(
            @RequestParam(required = false) String fundType) {
        if (fundType != null && !fundType.isBlank()) {
            return ResponseEntity.ok(fundRepo.findByFundTypeIgnoreCase(fundType));
        }
        return ResponseEntity.ok(fundRepo.findAllByOrderByDividendYield12mDesc());
    }

    @GetMapping("/indicators/{symbol}")
    public ResponseEntity<?> getCurrent(@PathVariable String symbol) {
        return fundRepo.findById(symbol.toUpperCase())
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/analysis/{symbol}")
    public ResponseEntity<?> getAnalysis(@PathVariable String symbol) {
        return analysisService.getAnalysis(symbol)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/analysis/{symbol}/history")
    public ResponseEntity<List<FundNavHistoryJpaEntity>> getNavHistory(@PathVariable String symbol) {
        return ResponseEntity.ok(
                navHistoryRepo.findBySymbolOrderByReferenceDateAsc(symbol.toUpperCase()));
    }

    @GetMapping("/analysis/{symbol}/dividends")
    public ResponseEntity<List<FundDividendEventJpaEntity>> getDividends(@PathVariable String symbol) {
        return ResponseEntity.ok(
                dividendRepo.findBySymbolOrderByPaymentDateDesc(symbol.toUpperCase()));
    }
}
