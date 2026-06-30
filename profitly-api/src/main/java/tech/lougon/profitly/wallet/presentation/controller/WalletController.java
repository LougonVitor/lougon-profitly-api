package tech.lougon.profitly.wallet.presentation.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tech.lougon.profitly.wallet.application.service.WalletService;
import tech.lougon.profitly.wallet.presentation.request.AddEntryRequest;
import tech.lougon.profitly.wallet.presentation.request.CreateWalletRequest;
import tech.lougon.profitly.wallet.presentation.request.UpdateEntryRequest;
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
    public ResponseEntity<List<WalletSummaryResponse>> findAll(Authentication auth) {
        String userId = (String) auth.getPrincipal();
        List<WalletSummaryResponse> response = walletService.findAll(userId).stream()
                .map(WalletSummaryResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WalletSummaryResponse> findById(@PathVariable String id) {
        return ResponseEntity.ok(WalletSummaryResponse.from(walletService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<WalletSummaryResponse> create(
            @RequestBody @Valid CreateWalletRequest request,
            Authentication auth
    ) {
        String userId = (String) auth.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(WalletSummaryResponse.from(walletService.create(request.name(), userId)));
    }

    @DeleteMapping("/{walletId}")
    public ResponseEntity<Void> deleteWallet(@PathVariable String walletId) {
        walletService.deleteWallet(walletId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{walletId}/positions/{ticker}/entries")
    public ResponseEntity<WalletSummaryResponse> addEntry(
            @PathVariable String walletId,
            @PathVariable String ticker,
            @RequestBody AddEntryRequest request
    ) {
        return ResponseEntity.ok(WalletSummaryResponse.from(walletService.addEntry(walletId, ticker, request)));
    }

    @PutMapping("/{walletId}/positions/{ticker}/entries/{entryId}")
    public ResponseEntity<WalletSummaryResponse> updateEntry(
            @PathVariable String walletId,
            @PathVariable String ticker,
            @PathVariable String entryId,
            @RequestBody UpdateEntryRequest request
    ) {
        return ResponseEntity.ok(WalletSummaryResponse.from(walletService.updateEntry(walletId, entryId, request)));
    }

    @DeleteMapping("/{walletId}/positions/{ticker}/entries/{entryId}")
    public ResponseEntity<WalletSummaryResponse> deleteEntry(
            @PathVariable String walletId,
            @PathVariable String ticker,
            @PathVariable String entryId
    ) {
        return ResponseEntity.ok(WalletSummaryResponse.from(walletService.deleteEntry(walletId, entryId)));
    }

    @DeleteMapping("/{walletId}/positions/{ticker}")
    public ResponseEntity<WalletSummaryResponse> deletePosition(
            @PathVariable String walletId,
            @PathVariable String ticker
    ) {
        return ResponseEntity.ok(WalletSummaryResponse.from(walletService.deletePosition(walletId, ticker)));
    }
}
