package tech.lougon.profitly.wallet.presentation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.lougon.profitly.wallet.application.service.WalletService;
import tech.lougon.profitly.wallet.presentation.response.WalletSummaryResponse;

import java.util.List;

@RestController
@RequestMapping("/api/wallets")
@CrossOrigin(origins = "http://localhost:5173")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping
    public ResponseEntity<List<WalletSummaryResponse>> findAll() {
        List<WalletSummaryResponse> response = walletService.findAll().stream()
                .map(WalletSummaryResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WalletSummaryResponse> findById(@PathVariable String id) {
        return walletService.findById(id)
                .map(WalletSummaryResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
