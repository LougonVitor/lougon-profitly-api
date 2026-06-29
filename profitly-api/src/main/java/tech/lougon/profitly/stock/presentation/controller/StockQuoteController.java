package tech.lougon.profitly.stock.presentation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.lougon.profitly.stock.application.service.StockQuoteService;
import tech.lougon.profitly.stock.presentation.response.StockQuoteResponse;

import java.util.List;

@RestController
@RequestMapping("/api/stocks/quote")
public class StockQuoteController {

    private final StockQuoteService stockService;

    public StockQuoteController(StockQuoteService stockService) {
        this.stockService = stockService;
    }

    @GetMapping
    public ResponseEntity<List<StockQuoteResponse>> findAll() {
        List<StockQuoteResponse> response = stockService.findAll()
                .stream()
                .map(StockQuoteResponse::from)
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{ticker}")
    public ResponseEntity<StockQuoteResponse> findByTicker(@PathVariable String ticker) {
        return stockService.findByTicker(ticker.toUpperCase())
                .map(StockQuoteResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}