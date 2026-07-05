package tech.lougon.profitly.ticker.presentation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.lougon.profitly.ticker.application.service.TickerService;
import tech.lougon.profitly.ticker.presentation.response.TickerResponse;

import java.util.List;

@RestController
@RequestMapping("/api/tickers")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5199"})
public class TickerController {

    private final TickerService tickerService;

    public TickerController(TickerService tickerService) {
        this.tickerService = tickerService;
    }

    @GetMapping
    public ResponseEntity<List<TickerResponse>> findAll() {
        List<TickerResponse> response = tickerService.findAll().stream()
                .map(TickerResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }
}
