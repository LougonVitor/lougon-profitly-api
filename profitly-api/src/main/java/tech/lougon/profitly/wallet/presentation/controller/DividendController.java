package tech.lougon.profitly.wallet.presentation.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tech.lougon.profitly.wallet.application.service.DividendService;
import tech.lougon.profitly.wallet.domain.model.Dividend;
import tech.lougon.profitly.wallet.presentation.request.AddDividendRequest;

import java.util.List;


@RestController
@RequestMapping("/api/wallets/{walletId}/dividends")
public class DividendController {

    private final DividendService service;

    public DividendController(DividendService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<Dividend>> list(@AuthenticationPrincipal String userId,
                                               @PathVariable String walletId) {
        return ResponseEntity.ok(service.findByWallet(walletId, userId));
    }

    @PostMapping
    public ResponseEntity<Dividend> add(@AuthenticationPrincipal String userId,
                                         @PathVariable String walletId,
                                         @Valid @RequestBody AddDividendRequest req) {
        return ResponseEntity.ok(service.add(walletId, userId, req));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<Dividend> toggle(@AuthenticationPrincipal String userId,
                                            @PathVariable String walletId,
                                            @PathVariable String id) {
        return ResponseEntity.ok(service.toggleReceived(id, userId));
    }

    @PostMapping("/sync")
    public ResponseEntity<List<Dividend>> sync(@AuthenticationPrincipal String userId,
                                               @PathVariable String walletId) {
        return ResponseEntity.ok(service.syncFromMarket(walletId, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal String userId,
                                        @PathVariable String walletId,
                                        @PathVariable String id) {
        service.delete(id, userId);
        return ResponseEntity.noContent().build();
    }
}
