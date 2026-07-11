package tech.lougon.profitly.wallet.presentation.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private final tech.lougon.profitly.wallet.application.service.WalletEvolutionService evolutionService;

    public WalletController(WalletService walletService,
                            tech.lougon.profitly.wallet.application.service.WalletEvolutionService evolutionService) {
        this.walletService = walletService;
        this.evolutionService = evolutionService;
    }

    @GetMapping("/{walletId}/evolution")
    public ResponseEntity<List<tech.lougon.profitly.wallet.application.service.WalletEvolutionService.EvolutionPoint>> evolution(
            @AuthenticationPrincipal String userId,
            @PathVariable String walletId
    ) {
        return ResponseEntity.ok(evolutionService.evolution(walletId, userId));
    }

    @GetMapping
    public ResponseEntity<List<WalletSummaryResponse>> findAll(@AuthenticationPrincipal String userId) {
        List<WalletSummaryResponse> response = walletService.findAll(userId).stream()
                .map(WalletSummaryResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WalletSummaryResponse> findById(@AuthenticationPrincipal String userId,
                                                          @PathVariable String id) {
        return ResponseEntity.ok(WalletSummaryResponse.from(walletService.findById(id, userId)));
    }

    @PostMapping
    public ResponseEntity<WalletSummaryResponse> create(
            @RequestBody @Valid CreateWalletRequest request,
            @AuthenticationPrincipal String userId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(WalletSummaryResponse.from(walletService.create(request.name(), userId)));
    }

    @PatchMapping("/{walletId}/name")
    public ResponseEntity<WalletSummaryResponse> rename(
            @AuthenticationPrincipal String userId,
            @PathVariable String walletId,
            @RequestBody java.util.Map<String, String> body
    ) {
        return ResponseEntity.ok(WalletSummaryResponse.from(walletService.rename(walletId, body.get("name"), userId)));
    }

    @DeleteMapping("/{walletId}")
    public ResponseEntity<Void> deleteWallet(@AuthenticationPrincipal String userId,
                                             @PathVariable String walletId) {
        walletService.deleteWallet(walletId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{walletId}/positions/{ticker}/entries")
    public ResponseEntity<WalletSummaryResponse> addEntry(
            @AuthenticationPrincipal String userId,
            @PathVariable String walletId,
            @PathVariable String ticker,
            @RequestBody AddEntryRequest request
    ) {
        return ResponseEntity.ok(WalletSummaryResponse.from(walletService.addEntry(walletId, ticker, request, userId)));
    }

    @PutMapping("/{walletId}/positions/{ticker}/entries/{entryId}")
    public ResponseEntity<WalletSummaryResponse> updateEntry(
            @AuthenticationPrincipal String userId,
            @PathVariable String walletId,
            @PathVariable String ticker,
            @PathVariable String entryId,
            @RequestBody UpdateEntryRequest request
    ) {
        return ResponseEntity.ok(WalletSummaryResponse.from(walletService.updateEntry(walletId, entryId, request, userId)));
    }

    @DeleteMapping("/{walletId}/positions/{ticker}/entries/{entryId}")
    public ResponseEntity<WalletSummaryResponse> deleteEntry(
            @AuthenticationPrincipal String userId,
            @PathVariable String walletId,
            @PathVariable String ticker,
            @PathVariable String entryId
    ) {
        return ResponseEntity.ok(WalletSummaryResponse.from(walletService.deleteEntry(walletId, entryId, userId)));
    }

    @DeleteMapping("/{walletId}/positions/{ticker}")
    public ResponseEntity<WalletSummaryResponse> deletePosition(
            @AuthenticationPrincipal String userId,
            @PathVariable String walletId,
            @PathVariable String ticker
    ) {
        return ResponseEntity.ok(WalletSummaryResponse.from(walletService.deletePosition(walletId, ticker, userId)));
    }
}
