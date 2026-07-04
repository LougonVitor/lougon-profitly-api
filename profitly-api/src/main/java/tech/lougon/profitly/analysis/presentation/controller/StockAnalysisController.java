package tech.lougon.profitly.analysis.presentation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.lougon.profitly.analysis.application.service.StockAnalysisService;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Advanced stock analysis — all data served from the database (no brapi calls). */
@RestController
@RequestMapping("/api/stocks")
public class StockAnalysisController {

    private static final Set<String> STATEMENT_TYPES =
            Set.of("balance_sheet", "income_statement", "cash_flow", "value_added");

    private final StockAnalysisService service;

    public StockAnalysisController(StockAnalysisService service) {
        this.service = service;
    }

    /** Everything the analysis page needs in one call. */
    @GetMapping("/{symbol}/analysis")
    public ResponseEntity<Map<String, Object>> getFullAnalysis(@PathVariable String symbol) {
        String s = symbol.toUpperCase();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("quote", service.getQuote(s).orElse(null));
        body.put("profile", service.getProfile(s).orElse(null));
        body.put("financials", service.getFinancials(s).orElse(null));
        body.put("dividends", service.getDividendAnalysis(s));
        body.put("sectorComparison", service.getSectorComparison(s).orElse(null));
        body.put("keyIndicators", service.getKeyIndicators(s));
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{symbol}/indicators")
    public ResponseEntity<Map<String, Double>> getKeyIndicators(@PathVariable String symbol) {
        return ResponseEntity.ok(service.getKeyIndicators(symbol.toUpperCase()));
    }

    @GetMapping("/{symbol}/quote")
    public ResponseEntity<?> getQuote(@PathVariable String symbol) {
        return service.getQuote(symbol.toUpperCase())
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{symbol}/profile")
    public ResponseEntity<?> getProfile(@PathVariable String symbol) {
        return service.getProfile(symbol.toUpperCase())
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{symbol}/financials")
    public ResponseEntity<?> getFinancials(@PathVariable String symbol) {
        return service.getFinancials(symbol.toUpperCase())
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** type: balance_sheet | income_statement | cash_flow | value_added */
    @GetMapping("/{symbol}/statements/{type}")
    public ResponseEntity<?> getStatements(@PathVariable String symbol, @PathVariable String type) {
        if (!STATEMENT_TYPES.contains(type)) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    "type must be one of " + STATEMENT_TYPES));
        }
        return ResponseEntity.ok(service.getStatements(symbol.toUpperCase(), type));
    }

    @GetMapping("/{symbol}/dividends")
    public ResponseEntity<Map<String, Object>> getDividends(@PathVariable String symbol) {
        return ResponseEntity.ok(service.getDividendAnalysis(symbol.toUpperCase()));
    }

    @GetMapping("/{symbol}/sector-comparison")
    public ResponseEntity<?> getSectorComparison(@PathVariable String symbol) {
        return service.getSectorComparison(symbol.toUpperCase())
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
