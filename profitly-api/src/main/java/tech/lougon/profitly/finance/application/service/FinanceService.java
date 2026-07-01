package tech.lougon.profitly.finance.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.lougon.profitly.finance.domain.model.*;
import tech.lougon.profitly.finance.domain.repository.*;
import tech.lougon.profitly.finance.presentation.request.*;
import tech.lougon.profitly.finance.application.dto.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FinanceService {

    private static final DateTimeFormatter YM_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final ExpenseRepository expenseRepository;
    private final FinanceSettingsRepository settingsRepository;
    private final ExpenseHistoryRepository historyRepository;
    private final RecurringExpenseRepository recurringExpenseRepository;
    private final AdditionalIncomeRepository additionalIncomeRepository;

    public FinanceService(ExpenseRepository expenseRepository,
                          FinanceSettingsRepository settingsRepository,
                          ExpenseHistoryRepository historyRepository,
                          RecurringExpenseRepository recurringExpenseRepository,
                          AdditionalIncomeRepository additionalIncomeRepository) {
        this.expenseRepository = expenseRepository;
        this.settingsRepository = settingsRepository;
        this.historyRepository = historyRepository;
        this.recurringExpenseRepository = recurringExpenseRepository;
        this.additionalIncomeRepository = additionalIncomeRepository;
    }

    @Transactional
    public CurrentPeriodDTO getCurrentPeriod(String userId) {
        var settings = getOrCreateSettings(userId);
        var expenses = expenseRepository.findByUserId(userId);

        // Auto-populate recurring expenses not yet in current period
        var recurringList = recurringExpenseRepository.findByUserId(userId);
        Set<String> existingTitles = expenses.stream()
                .map(e -> e.title().toLowerCase())
                .collect(Collectors.toSet());

        for (RecurringExpense recurring : recurringList) {
            if (!existingTitles.contains(recurring.title().toLowerCase())) {
                var newExpense = new Expense(null, userId, recurring.title(),
                        recurring.estimatedValue(), BigDecimal.ZERO,
                        ExpenseStatus.PENDING, recurring.type(), Instant.now(), true);
                expenseRepository.save(newExpense);
            }
        }

        // Ensure INVESTMENT expense exists
        var investmentExpenses = expenses.stream()
                .filter(e -> e.type() == ExpenseType.INVESTMENT)
                .toList();
        if (investmentExpenses.isEmpty()) {
            var investmentExpense = new Expense(null, userId, "Investimento",
                    null, BigDecimal.ZERO, ExpenseStatus.PENDING,
                    ExpenseType.INVESTMENT, Instant.now(), true);
            expenseRepository.save(investmentExpense);
        }

        // Reload after auto-population
        var updatedExpenses = expenseRepository.findByUserId(userId);
        var additionalIncomes = additionalIncomeRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return CurrentPeriodDTO.from(updatedExpenses, settings, additionalIncomes);
    }

    public ExpenseDTO addExpense(String userId, AddExpenseRequest req) {
        var expense = new Expense(null, userId, req.title(),
                req.estimatedValue(), req.realValue() != null ? req.realValue() : BigDecimal.ZERO,
                req.status() != null ? req.status() : ExpenseStatus.PENDING,
                req.type(), Instant.now(), req.recurring());
        return ExpenseDTO.from(expenseRepository.save(expense));
    }

    public ExpenseDTO quickLaunch(String userId, QuickLaunchRequest req) {
        var existing = expenseRepository.findByUserIdAndTitle(userId, req.title())
                .orElseThrow(() -> new IllegalArgumentException("Lançamento não encontrado: " + req.title()));

        BigDecimal newReal = existing.realValue().add(req.value());
        BigDecimal estimated = existing.estimatedValue();
        ExpenseStatus status = computeStatus(newReal, estimated);

        var updated = new Expense(existing.id(), existing.userId(), existing.title(),
                estimated, newReal, status, existing.type(), existing.createdAt(), existing.recurring());
        return ExpenseDTO.from(expenseRepository.save(updated));
    }

    public ExpenseDTO updateExpense(String userId, Long id, UpdateExpenseRequest req) {
        var existing = expenseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lançamento não encontrado"));
        if (!existing.userId().equals(userId)) throw new IllegalArgumentException("Acesso negado");

        BigDecimal realValue = req.realValue() != null ? req.realValue() : existing.realValue();
        BigDecimal estimated = req.estimatedValue() != null ? req.estimatedValue() : existing.estimatedValue();
        ExpenseStatus status = req.status() != null ? req.status() : computeStatus(realValue, estimated);

        var updated = new Expense(id, userId,
                req.title() != null ? req.title() : existing.title(),
                estimated, realValue, status,
                req.type() != null ? req.type() : existing.type(),
                existing.createdAt(), existing.recurring());
        return ExpenseDTO.from(expenseRepository.save(updated));
    }

    public void deleteExpense(String userId, Long id) {
        var existing = expenseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lançamento não encontrado"));
        if (!existing.userId().equals(userId)) throw new IllegalArgumentException("Acesso negado");
        expenseRepository.deleteById(id);
    }

    public FinanceSettings updateSettings(String userId, FinanceSettingsRequest req) {
        var settings = new FinanceSettings(userId,
                req.resetDay() != null ? req.resetDay() : 10,
                req.netSalary(), req.investmentTarget());
        return settingsRepository.save(settings);
    }

    public FinanceSettings getSettings(String userId) {
        return getOrCreateSettings(userId);
    }

    public HistoryDTO getHistory(String userId, String from, String to) {
        String fromYM = from != null ? from : YM_FMT.format(YearMonth.now().minusMonths(11));
        String toYM = to != null ? to : YM_FMT.format(YearMonth.now());
        var summaries = historyRepository.findByUserIdAndYearMonthBetween(userId, fromYM, toYM);
        var months = historyRepository.findDistinctYearMonthsByUserId(userId);
        return HistoryDTO.from(summaries, months);
    }

    public void resetPeriod(String userId) {
        var expenses = expenseRepository.findByUserId(userId);
        if (expenses.isEmpty()) return;

        String yearMonth = YM_FMT.format(YearMonth.now());

        Map<ExpenseType, BigDecimal[]> grouped = new EnumMap<>(ExpenseType.class);
        for (Expense e : expenses) {
            grouped.computeIfAbsent(e.type(), k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            grouped.get(e.type())[0] = grouped.get(e.type())[0].add(e.realValue());
            if (e.estimatedValue() != null)
                grouped.get(e.type())[1] = grouped.get(e.type())[1].add(e.estimatedValue());
        }

        List<ExpenseHistorySummary> summaries = grouped.entrySet().stream()
                .map(entry -> new ExpenseHistorySummary(null, userId, yearMonth,
                        entry.getKey(), entry.getValue()[0], entry.getValue()[1]))
                .toList();

        historyRepository.saveAll(summaries);

        for (Expense e : expenses) expenseRepository.deleteById(e.id());
    }

    public void checkAndResetIfDue(String userId) {
        var settings = getOrCreateSettings(userId);
        int today = LocalDate.now().getDayOfMonth();
        if (today == settings.resetDay()) resetPeriod(userId);
    }

    // Recurring expense methods

    public List<RecurringExpense> getRecurring(String userId) {
        return recurringExpenseRepository.findByUserId(userId);
    }

    public RecurringExpense saveRecurring(String userId, RecurringExpenseRequest req) {
        var recurring = new RecurringExpense(null, userId, req.title(), req.estimatedValue(), req.type());
        return recurringExpenseRepository.save(recurring);
    }

    @Transactional
    public void deleteRecurring(String userId, Long id) {
        var recurring = recurringExpenseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recorrente não encontrado"));
        if (!recurring.userId().equals(userId)) throw new IllegalArgumentException("Acesso negado");

        // Also delete matching expense in current period (case-insensitive title match)
        expenseRepository.findByUserIdAndTitle(userId, recurring.title())
                .ifPresent(e -> expenseRepository.deleteById(e.id()));

        recurringExpenseRepository.deleteById(id);
    }

    // Additional income methods

    public AdditionalIncome addIncome(String userId, AddIncomeRequest req) {
        var income = new AdditionalIncome(null, userId, req.description(), req.amount(), Instant.now());
        return additionalIncomeRepository.save(income);
    }

    public void deleteIncome(String userId, Long id) {
        additionalIncomeRepository.deleteById(id);
    }

    private FinanceSettings getOrCreateSettings(String userId) {
        return settingsRepository.findByUserId(userId)
                .orElseGet(() -> settingsRepository.save(new FinanceSettings(userId, 10, null, null)));
    }

    private ExpenseStatus computeStatus(BigDecimal real, BigDecimal estimated) {
        if (estimated == null || estimated.compareTo(BigDecimal.ZERO) == 0) return ExpenseStatus.PAID;
        int cmp = real.compareTo(estimated);
        if (cmp > 0) return ExpenseStatus.OVERRUN;
        if (cmp == 0) return ExpenseStatus.PAID;
        if (real.compareTo(BigDecimal.ZERO) > 0) return ExpenseStatus.PARTIAL;
        return ExpenseStatus.PENDING;
    }
}
