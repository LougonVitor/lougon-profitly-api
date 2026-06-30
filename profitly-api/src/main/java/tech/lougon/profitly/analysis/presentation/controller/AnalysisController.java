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

    @GetMapping("/{symbol}")
    public ResponseEntity<TickerAnalysisResponse> getAnalysis(@PathVariable String symbol) {
        try {
            var dto = analysisService.getAnalysis(symbol.toUpperCase());
            return ResponseEntity.ok(TickerAnalysisResponse.from(dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{symbol}/history")
    public ResponseEntity<PriceHistoryResponse> getHistory(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "5y") String range) {
        var points = analysisService.getPriceHistory(symbol.toUpperCase(), range);
        return ResponseEntity.ok(PriceHistoryResponse.from(symbol.toUpperCase(), range, points));
    }
}
