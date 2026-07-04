package tech.lougon.profitly.analysis.presentation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.lougon.profitly.analysis.infrastructure.persistence.CryptoCoinJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.CryptoQuoteJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaCryptoCoinRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaCryptoQuoteRepository;

import java.util.List;

@RestController
@RequestMapping("/api/crypto")
public class CryptoController {

    private final JpaCryptoCoinRepository coinRepo;
    private final JpaCryptoQuoteRepository quoteRepo;

    public CryptoController(JpaCryptoCoinRepository coinRepo,
                             JpaCryptoQuoteRepository quoteRepo) {
        this.coinRepo = coinRepo;
        this.quoteRepo = quoteRepo;
    }

    @GetMapping("/coins")
    public ResponseEntity<List<CryptoCoinJpaEntity>> getCoins() {
        return ResponseEntity.ok(coinRepo.findAll());
    }

    @GetMapping("/quotes")
    public ResponseEntity<List<CryptoQuoteJpaEntity>> getQuotes() {
        return ResponseEntity.ok(quoteRepo.findAllByOrderByMarketCapDesc());
    }

    @GetMapping("/quotes/{coin}")
    public ResponseEntity<?> getQuote(@PathVariable String coin) {
        return quoteRepo.findById(coin.toUpperCase())
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
