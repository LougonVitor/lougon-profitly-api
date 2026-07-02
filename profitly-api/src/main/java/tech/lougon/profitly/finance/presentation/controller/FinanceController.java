package tech.lougon.profitly.finance.presentation.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tech.lougon.profitly.finance.application.dto.*;
import tech.lougon.profitly.finance.application.service.FinanceService;
import tech.lougon.profitly.finance.domain.model.AdditionalIncome;
import tech.lougon.profitly.finance.domain.model.FinanceSettings;
import tech.lougon.profitly.finance.domain.model.RecurringExpense;
import tech.lougon.profitly.finance.presentation.request.*;

import java.util.List;

@RestController
@RequestMapping("/api/finance")
public class FinanceController {

    private final FinanceService financeService;

    public FinanceController(FinanceService financeService) {
        this.financeService = financeService;
    }

    @GetMapping("/current")
    public ResponseEntity<CurrentPeriodDTO> getCurrent(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(financeService.getCurrentPeriod(userId));
    }

    @PostMapping("/expenses")
    public ResponseEntity<ExpenseDTO> addExpense(@AuthenticationPrincipal String userId,
                                                  @Valid @RequestBody AddExpenseRequest req) {
        return ResponseEntity.ok(financeService.addExpense(userId, req));
    }

    @PostMapping("/expenses/quick-launch")
    public ResponseEntity<ExpenseDTO> quickLaunch(@AuthenticationPrincipal String userId,
                                                   @Valid @RequestBody QuickLaunchRequest req) {
        return ResponseEntity.ok(financeService.quickLaunch(userId, req));
    }

    @PatchMapping("/expenses/{id}")
    public ResponseEntity<ExpenseDTO> updateExpense(@AuthenticationPrincipal String userId,
                                                     @PathVariable Long id,
                                                     @RequestBody UpdateExpenseRequest req) {
        return ResponseEntity.ok(financeService.updateExpense(userId, id, req));
    }

    @DeleteMapping("/expenses/{id}")
    public ResponseEntity<Void> deleteExpense(@AuthenticationPrincipal String userId,
                                               @PathVariable Long id) {
        financeService.deleteExpense(userId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/settings")
    public ResponseEntity<FinanceSettings> getSettings(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(financeService.getSettings(userId));
    }

    @PutMapping("/settings")
    public ResponseEntity<FinanceSettings> updateSettings(@AuthenticationPrincipal String userId,
                                                           @RequestBody FinanceSettingsRequest req) {
        return ResponseEntity.ok(financeService.updateSettings(userId, req));
    }

    @GetMapping("/history")
    public ResponseEntity<HistoryDTO> getHistory(@AuthenticationPrincipal String userId,
                                                  @RequestParam(required = false) String from,
                                                  @RequestParam(required = false) String to) {
        return ResponseEntity.ok(financeService.getHistory(userId, from, to));
    }

    @GetMapping("/reset/check")
    public ResponseEntity<Boolean> checkResetConflict(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(financeService.hasPeriodConflict(userId));
    }

    @PostMapping("/reset")
    public ResponseEntity<Void> resetPeriod(@AuthenticationPrincipal String userId) {
        financeService.resetPeriod(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/recurring")
    public ResponseEntity<List<RecurringExpense>> getRecurring(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(financeService.getRecurring(userId));
    }

    @PostMapping("/recurring")
    public ResponseEntity<RecurringExpense> saveRecurring(@AuthenticationPrincipal String userId,
                                                           @Valid @RequestBody RecurringExpenseRequest req) {
        return ResponseEntity.ok(financeService.saveRecurring(userId, req));
    }

    @DeleteMapping("/recurring/{id}")
    public ResponseEntity<Void> deleteRecurring(@AuthenticationPrincipal String userId,
                                                 @PathVariable Long id) {
        financeService.deleteRecurring(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/income")
    public ResponseEntity<AdditionalIncome> addIncome(@AuthenticationPrincipal String userId,
                                                       @Valid @RequestBody AddIncomeRequest req) {
        return ResponseEntity.ok(financeService.addIncome(userId, req));
    }

    @DeleteMapping("/income/{id}")
    public ResponseEntity<Void> deleteIncome(@AuthenticationPrincipal String userId,
                                              @PathVariable Long id) {
        financeService.deleteIncome(userId, id);
        return ResponseEntity.noContent().build();
    }
}
