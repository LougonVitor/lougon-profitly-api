package tech.lougon.profitly.finance.presentation.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tech.lougon.profitly.finance.application.dto.*;
import tech.lougon.profitly.finance.application.service.FinanceService;
import tech.lougon.profitly.finance.domain.model.ExpenseType;
import tech.lougon.profitly.finance.presentation.request.*;
import tech.lougon.profitly.finance.presentation.response.AdditionalIncomeResponse;
import tech.lougon.profitly.finance.presentation.response.FinanceSettingsResponse;
import tech.lougon.profitly.finance.presentation.response.RecurringExpenseResponse;
import tech.lougon.profitly.finance.presentation.response.RecurringIncomeResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

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
    public ResponseEntity<FinanceSettingsResponse> getSettings(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(FinanceSettingsResponse.from(financeService.getSettings(userId)));
    }

    @PutMapping("/settings")
    public ResponseEntity<FinanceSettingsResponse> updateSettings(@AuthenticationPrincipal String userId,
                                                                  @RequestBody FinanceSettingsRequest req) {
        return ResponseEntity.ok(FinanceSettingsResponse.from(financeService.updateSettings(userId, req)));
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
    public ResponseEntity<List<RecurringExpenseResponse>> getRecurring(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(financeService.getRecurring(userId).stream()
                .map(RecurringExpenseResponse::from).toList());
    }

    @PostMapping("/recurring")
    public ResponseEntity<RecurringExpenseResponse> saveRecurring(@AuthenticationPrincipal String userId,
                                                                  @Valid @RequestBody RecurringExpenseRequest req) {
        return ResponseEntity.ok(RecurringExpenseResponse.from(financeService.saveRecurring(userId, req)));
    }

    @PutMapping("/recurring/{id}")
    public ResponseEntity<RecurringExpenseResponse> updateRecurring(@AuthenticationPrincipal String userId,
                                                                    @PathVariable Long id,
                                                                    @Valid @RequestBody RecurringExpenseRequest req) {
        return ResponseEntity.ok(RecurringExpenseResponse.from(financeService.updateRecurring(userId, id, req)));
    }

    @DeleteMapping("/recurring/{id}")
    public ResponseEntity<Void> deleteRecurring(@AuthenticationPrincipal String userId,
                                                 @PathVariable Long id) {
        financeService.deleteRecurring(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/income")
    public ResponseEntity<AdditionalIncomeResponse> addIncome(@AuthenticationPrincipal String userId,
                                                              @Valid @RequestBody AddIncomeRequest req) {
        return ResponseEntity.ok(AdditionalIncomeResponse.from(financeService.addIncome(userId, req)));
    }

    @DeleteMapping("/income/{id}")
    public ResponseEntity<Void> deleteIncome(@AuthenticationPrincipal String userId,
                                              @PathVariable Long id) {
        financeService.deleteIncome(userId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/recurring-income")
    public ResponseEntity<List<RecurringIncomeResponse>> getRecurringIncome(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(financeService.getRecurringIncome(userId).stream()
                .map(RecurringIncomeResponse::from).toList());
    }

    @PostMapping("/recurring-income")
    public ResponseEntity<RecurringIncomeResponse> saveRecurringIncome(@AuthenticationPrincipal String userId,
                                                                       @Valid @RequestBody RecurringIncomeRequest req) {
        return ResponseEntity.ok(RecurringIncomeResponse.from(financeService.saveRecurringIncome(userId, req)));
    }

    @PutMapping("/recurring-income/{id}")
    public ResponseEntity<RecurringIncomeResponse> updateRecurringIncome(@AuthenticationPrincipal String userId,
                                                                         @PathVariable Long id,
                                                                         @Valid @RequestBody RecurringIncomeRequest req) {
        return ResponseEntity.ok(RecurringIncomeResponse.from(financeService.updateRecurringIncome(userId, id, req)));
    }

    @DeleteMapping("/recurring-income/{id}")
    public ResponseEntity<Void> deleteRecurringIncome(@AuthenticationPrincipal String userId,
                                                      @PathVariable Long id) {
        financeService.deleteRecurringIncome(userId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/export/current", produces = "text/csv")
    public ResponseEntity<byte[]> exportCurrent(@AuthenticationPrincipal String userId) {
        return csvResponse(financeService.exportCurrentCsv(userId), "periodo-atual.csv");
    }

    @GetMapping(value = "/export/history", produces = "text/csv")
    public ResponseEntity<byte[]> exportHistory(@AuthenticationPrincipal String userId) {
        return csvResponse(financeService.exportHistoryCsv(userId), "historico.csv");
    }

    @PostMapping("/import/expenses")
    public ResponseEntity<Map<String, Integer>> importExpenses(@AuthenticationPrincipal String userId,
                                                               @RequestParam("file") MultipartFile file) throws IOException {
        int imported = financeService.importExpensesCsv(userId, new String(file.getBytes(), StandardCharsets.UTF_8));
        return ResponseEntity.ok(Map.of("imported", imported));
    }

    private ResponseEntity<byte[]> csvResponse(String csv, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/budget-limits")
    public ResponseEntity<List<BudgetLimitDTO>> getBudgetLimits(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(financeService.getBudgetLimits(userId).stream()
                .map(BudgetLimitDTO::from).toList());
    }

    @PutMapping("/budget-limits")
    public ResponseEntity<BudgetLimitDTO> saveBudgetLimit(@AuthenticationPrincipal String userId,
                                                          @Valid @RequestBody BudgetLimitRequest req) {
        return ResponseEntity.ok(BudgetLimitDTO.from(financeService.saveBudgetLimit(userId, req)));
    }

    @DeleteMapping("/budget-limits/{type}")
    public ResponseEntity<Void> deleteBudgetLimit(@AuthenticationPrincipal String userId,
                                                  @PathVariable ExpenseType type) {
        financeService.deleteBudgetLimit(userId, type);
        return ResponseEntity.noContent().build();
    }
}
