package tech.lougon.profitly.analysis.presentation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.lougon.profitly.analysis.infrastructure.persistence.FundIndicatorJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaFundIndicatorRepository;

import java.util.List;

@RestController
@RequestMapping("/api/funds")
public class FundIndicatorController {

    private final JpaFundIndicatorRepository fundRepo;

    public FundIndicatorController(JpaFundIndicatorRepository fundRepo) {
        this.fundRepo = fundRepo;
    }

    @GetMapping("/indicators")
    public ResponseEntity<List<FundIndicatorJpaEntity>> getAll() {
        return ResponseEntity.ok(fundRepo.findAllByOrderByDividendYield12mDesc());
    }

    @GetMapping("/indicators/{symbol}")
    public ResponseEntity<?> getCurrent(@PathVariable String symbol) {
        return fundRepo.findById(symbol.toUpperCase())
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
