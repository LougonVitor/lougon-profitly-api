package tech.lougon.profitly.analysis.presentation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.lougon.profitly.analysis.application.service.AnalysisService;
import tech.lougon.profitly.analysis.presentation.response.PriceHistoryResponse;
import tech.lougon.profitly.analysis.presentation.response.TickerAnalysisResponse;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    // Stock/crypto symbols are stored uppercase, but treasury symbols are lowercase
    // (e.g. "tesouro-prefixado-01012029") — try the symbol as sent, then uppercased.
    @GetMapping("/{symbol}")
    public ResponseEntity<TickerAnalysisResponse> getAnalysis(@PathVariable String symbol) {
        try {
            return ResponseEntity.ok(TickerAnalysisResponse.from(analysisService.getAnalysis(symbol)));
        } catch (IllegalArgumentException e) {
            try {
                return ResponseEntity.ok(TickerAnalysisResponse.from(analysisService.getAnalysis(symbol.toUpperCase())));
            } catch (IllegalArgumentException e2) {
                return ResponseEntity.notFound().build();
            }
        }
    }

    @GetMapping("/{symbol}/history")
    public ResponseEntity<PriceHistoryResponse> getHistory(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "5y") String range) {
        var points = analysisService.getPriceHistory(symbol, range);
        if (points.isEmpty() && !symbol.equals(symbol.toUpperCase())) {
            symbol = symbol.toUpperCase();
            points = analysisService.getPriceHistory(symbol, range);
        }
        return ResponseEntity.ok(PriceHistoryResponse.from(symbol, range, points));
    }
}
