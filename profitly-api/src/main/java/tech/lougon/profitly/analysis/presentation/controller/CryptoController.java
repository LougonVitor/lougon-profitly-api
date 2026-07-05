package tech.lougon.profitly.analysis.presentation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.lougon.profitly.analysis.application.service.CryptoAnalysisService;
import tech.lougon.profitly.analysis.infrastructure.persistence.CryptoCoinJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.CryptoFearGreedJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.CryptoQuoteJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaCryptoCoinRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaCryptoFearGreedRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaCryptoQuoteRepository;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/crypto")
public class CryptoController {

    private final JpaCryptoCoinRepository coinRepo;
    private final JpaCryptoQuoteRepository quoteRepo;
    private final CryptoAnalysisService analysisService;
    private final JpaCryptoFearGreedRepository fearGreedRepo;

    public CryptoController(JpaCryptoCoinRepository coinRepo,
                             JpaCryptoQuoteRepository quoteRepo,
                             CryptoAnalysisService analysisService,
                             JpaCryptoFearGreedRepository fearGreedRepo) {
        this.coinRepo = coinRepo;
        this.quoteRepo = quoteRepo;
        this.analysisService = analysisService;
        this.fearGreedRepo = fearGreedRepo;
    }

    @GetMapping("/coins")
    public ResponseEntity<List<CryptoCoinJpaEntity>> getCoins() {
        return ResponseEntity.ok(coinRepo.findAll());
    }

    @GetMapping("/quotes")
    public ResponseEntity<List<CryptoQuoteJpaEntity>> getQuotes() {
        return ResponseEntity.ok(quoteRepo.findAllByOrderByVolumeDesc());
    }

    @GetMapping("/quotes/{coin}")
    public ResponseEntity<?> getQuote(@PathVariable String coin) {
        return quoteRepo.findById(coin.toUpperCase())
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Quote enriched with indicators computed from stored price history. */
    @GetMapping("/analysis/{coin}")
    public ResponseEntity<?> getAnalysis(@PathVariable String coin) {
        return analysisService.getAnalysis(coin)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Crypto Fear & Greed Index readings for the last {@code days} days, oldest first. */
    @GetMapping("/fear-greed")
    public ResponseEntity<List<CryptoFearGreedJpaEntity>> getFearGreed(
            @RequestParam(defaultValue = "90") int days) {
        LocalDate from = LocalDate.now().minusDays(Math.max(1, days));
        return ResponseEntity.ok(fearGreedRepo.findByDateGreaterThanEqualOrderByDateAsc(from));
    }
}
