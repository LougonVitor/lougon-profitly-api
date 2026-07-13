package tech.lougon.profitly.wallet.presentation.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tech.lougon.profitly.wallet.application.service.B3StatementImportService;
import tech.lougon.profitly.wallet.application.service.FixedIncomeService;
import tech.lougon.profitly.wallet.application.service.WalletService;
import tech.lougon.profitly.wallet.presentation.request.AddEntryRequest;
import tech.lougon.profitly.wallet.presentation.request.AddFixedIncomeEntryRequest;
import tech.lougon.profitly.wallet.presentation.request.CreateWalletRequest;
import tech.lougon.profitly.wallet.presentation.request.RedeemFixedIncomeRequest;
import tech.lougon.profitly.wallet.presentation.request.UpdateEntryRequest;
import tech.lougon.profitly.wallet.presentation.response.B3ImportResultResponse;
import tech.lougon.profitly.wallet.presentation.response.WalletSummaryResponse;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/wallets")
@CrossOrigin(origins = "http://localhost:5173")
public class WalletController {

    private final WalletService walletService;
    private final FixedIncomeService fixedIncomeService;
    private final tech.lougon.profitly.wallet.application.service.WalletEvolutionService evolutionService;
    private final B3StatementImportService b3StatementImportService;

    public WalletController(WalletService walletService,
                            FixedIncomeService fixedIncomeService,
                            tech.lougon.profitly.wallet.application.service.WalletEvolutionService evolutionService,
                            B3StatementImportService b3StatementImportService) {
        this.walletService = walletService;
        this.fixedIncomeService = fixedIncomeService;
        this.evolutionService = evolutionService;
        this.b3StatementImportService = b3StatementImportService;
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

    @PostMapping("/{walletId}/import/b3")
    public ResponseEntity<B3ImportResultResponse> importB3Statement(
            @AuthenticationPrincipal String userId,
            @PathVariable String walletId,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        walletService.findById(walletId, userId); // enforces ownership, 404s otherwise
        var result = b3StatementImportService.importStatement(walletId, userId, file.getInputStream());
        return ResponseEntity.ok(B3ImportResultResponse.from(result));
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

    @PostMapping("/{walletId}/fixed-income")
    public ResponseEntity<WalletSummaryResponse> addFixedIncomeEntry(
            @AuthenticationPrincipal String userId,
            @PathVariable String walletId,
            @RequestBody @Valid AddFixedIncomeEntryRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(WalletSummaryResponse.from(fixedIncomeService.createPosition(walletId, request, userId)));
    }

    @PostMapping("/{walletId}/positions/{ticker}/redeem")
    public ResponseEntity<WalletSummaryResponse> redeemFixedIncome(
            @AuthenticationPrincipal String userId,
            @PathVariable String walletId,
            @PathVariable String ticker,
            @RequestBody @Valid RedeemFixedIncomeRequest request
    ) {
        return ResponseEntity.ok(WalletSummaryResponse.from(fixedIncomeService.redeem(walletId, ticker, request, userId)));
    }
}
