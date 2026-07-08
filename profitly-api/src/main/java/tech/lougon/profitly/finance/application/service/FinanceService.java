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
import java.time.format.DateTimeParseException;
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
    private final BudgetLimitRepository budgetLimitRepository;
    private final RecurringIncomeRepository recurringIncomeRepository;

    public FinanceService(ExpenseRepository expenseRepository,
                          FinanceSettingsRepository settingsRepository,
                          ExpenseHistoryRepository historyRepository,
                          RecurringExpenseRepository recurringExpenseRepository,
                          AdditionalIncomeRepository additionalIncomeRepository,
                          BudgetLimitRepository budgetLimitRepository,
                          RecurringIncomeRepository recurringIncomeRepository) {
        this.expenseRepository = expenseRepository;
        this.settingsRepository = settingsRepository;
        this.historyRepository = historyRepository;
        this.recurringExpenseRepository = recurringExpenseRepository;
        this.additionalIncomeRepository = additionalIncomeRepository;
        this.budgetLimitRepository = budgetLimitRepository;
        this.recurringIncomeRepository = recurringIncomeRepository;
    }

    @Transactional
    public CurrentPeriodDTO getCurrentPeriod(String userId) {
        var settings = getOrCreateSettings(userId);
        var expenses = expenseRepository.findByUserId(userId);

        // Auto-populate recurring expenses not yet in current period.
        // Match on the template link (robust to renames); fall back to title for legacy
        // rows created before the link column existed.
        var recurringList = recurringExpenseRepository.findByUserId(userId);
        Set<Long> linkedRecurringIds = expenses.stream()
                .map(Expense::recurringExpenseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<String> existingTitles = expenses.stream()
                .map(e -> e.title().toLowerCase())
                .collect(Collectors.toSet());

        for (RecurringExpense recurring : recurringList) {
            boolean alreadyPresent = linkedRecurringIds.contains(recurring.id())
                    || existingTitles.contains(recurring.title().toLowerCase());
            if (!alreadyPresent) {
                var newExpense = new Expense(null, userId, recurring.title(),
                        recurring.estimatedValue(), BigDecimal.ZERO,
                        ExpenseStatus.PENDING, recurring.type(), Instant.now(), true, recurring.id());
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
                    ExpenseType.INVESTMENT, Instant.now(), true, null);
            expenseRepository.save(investmentExpense);
        }

        // Auto-inject recurring incomes not yet present this period (link, then legacy description)
        var recurringIncomes = recurringIncomeRepository.findByUserId(userId);
        var currentIncomes = additionalIncomeRepository.findByUserIdOrderByCreatedAtDesc(userId);
        Set<Long> linkedIncomeIds = currentIncomes.stream()
                .map(AdditionalIncome::recurringIncomeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<String> existingDescriptions = currentIncomes.stream()
                .map(i -> i.description().toLowerCase())
                .collect(Collectors.toSet());
        for (RecurringIncome ri : recurringIncomes) {
            boolean present = linkedIncomeIds.contains(ri.id())
                    || existingDescriptions.contains(ri.description().toLowerCase());
            if (!present) {
                additionalIncomeRepository.save(new AdditionalIncome(null, userId, ri.description(),
                        ri.amount(), Instant.now(), ri.id()));
            }
        }

        // Reload after auto-population
        var updatedExpenses = expenseRepository.findByUserId(userId);
        var additionalIncomes = additionalIncomeRepository.findByUserIdOrderByCreatedAtDesc(userId);
        var budgetLimits = budgetLimitRepository.findByUserId(userId);
        return CurrentPeriodDTO.from(updatedExpenses, settings, additionalIncomes, budgetLimits);
    }

    public ExpenseDTO addExpense(String userId, AddExpenseRequest req) {
        BigDecimal realValue = req.realValue() != null ? req.realValue() : BigDecimal.ZERO;
        var expense = new Expense(null, userId, req.title(),
                req.estimatedValue(), realValue,
                computeStatus(realValue, req.estimatedValue()),
                req.type(), Instant.now(), req.recurring(), null);
        return ExpenseDTO.from(expenseRepository.save(expense));
    }

    public ExpenseDTO quickLaunch(String userId, QuickLaunchRequest req) {
        var existing = expenseRepository.findByUserIdAndTitle(userId, req.title())
                .orElseThrow(() -> new IllegalArgumentException("Lançamento não encontrado: " + req.title()));

        BigDecimal newReal = existing.realValue().add(req.value());
        BigDecimal estimated = existing.estimatedValue();
        ExpenseStatus status = computeStatus(newReal, estimated);

        var updated = new Expense(existing.id(), existing.userId(), existing.title(),
                estimated, newReal, status, existing.type(), existing.createdAt(),
                existing.recurring(), existing.recurringExpenseId());
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
                existing.createdAt(), existing.recurring(), existing.recurringExpenseId());
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
        String fromYM = normalizeYearMonth(from, YM_FMT.format(YearMonth.now().minusMonths(11)));
        String toYM = normalizeYearMonth(to, YM_FMT.format(YearMonth.now()));
        var summaries = historyRepository.findByUserIdAndYearMonthBetween(userId, fromYM, toYM);
        var months = historyRepository.findDistinctYearMonthsByUserId(userId);
        return HistoryDTO.from(summaries, months);
    }

    private String normalizeYearMonth(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        try {
            return YM_FMT.format(YearMonth.parse(value, YM_FMT));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Formato de mês inválido (use yyyy-MM): " + value);
        }
    }

    @Transactional
    public void resetPeriod(String userId) {
        var expenses = expenseRepository.findByUserId(userId);
        if (expenses.isEmpty()) return;

        // Archive under the month in which the period is being closed
        String yearMonth = archiveYearMonth();

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

        // Replace existing data for this month if present
        historyRepository.deleteByUserIdAndYearMonth(userId, yearMonth);
        historyRepository.saveAll(summaries);

        // Retain a rolling 12 months of history (matches the default history window)
        String cutoff = YM_FMT.format(YearMonth.now().minusMonths(12));
        historyRepository.deleteOlderThan(userId, cutoff);

        expenseRepository.deleteAllByUserId(userId);
    }

    public boolean hasPeriodConflict(String userId) {
        return historyRepository.existsByUserIdAndYearMonth(userId, archiveYearMonth());
    }

    @Transactional
    public boolean checkAndResetIfDue(String userId) {
        var settings = getOrCreateSettings(userId);
        LocalDate now = LocalDate.now();
        // Clamp the reset day to the month length so short months (e.g. Feb) still fire.
        int effectiveResetDay = Math.min(settings.resetDay(), now.lengthOfMonth());
        if (now.getDayOfMonth() != effectiveResetDay) return false;
        resetPeriod(userId);
        return true;
    }

    private String archiveYearMonth() {
        return YM_FMT.format(YearMonth.now());
    }

    // Recurring expense methods

    public List<RecurringExpense> getRecurring(String userId) {
        return recurringExpenseRepository.findByUserId(userId);
    }

    public RecurringExpense saveRecurring(String userId, RecurringExpenseRequest req) {
        var recurring = new RecurringExpense(null, userId, req.title(), req.estimatedValue(), req.type(),
                req.dueDay(), req.variable());
        return recurringExpenseRepository.save(recurring);
    }

    @Transactional
    public void deleteRecurring(String userId, Long id) {
        var recurring = recurringExpenseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recorrente não encontrado"));
        if (!recurring.userId().equals(userId)) throw new IllegalArgumentException("Acesso negado");

        // Detach the current-period expense spawned by this template. Delete it only if
        // nothing has been recorded yet; otherwise keep it as a one-off so real spending
        // is never lost when a template is removed.
        expenseRepository.findByUserId(userId).stream()
                .filter(e -> id.equals(e.recurringExpenseId())
                        || (e.recurringExpenseId() == null && e.title().equalsIgnoreCase(recurring.title())))
                .findFirst()
                .ifPresent(e -> {
                    if (e.realValue() == null || e.realValue().compareTo(BigDecimal.ZERO) == 0) {
                        expenseRepository.deleteById(e.id());
                    } else {
                        expenseRepository.save(new Expense(e.id(), e.userId(), e.title(),
                                e.estimatedValue(), e.realValue(), e.status(), e.type(),
                                e.createdAt(), false, null));
                    }
                });

        recurringExpenseRepository.deleteById(id);
    }

    // Additional income methods

    public AdditionalIncome addIncome(String userId, AddIncomeRequest req) {
        var income = new AdditionalIncome(null, userId, req.description(), req.amount(), Instant.now(), null);
        return additionalIncomeRepository.save(income);
    }

    public void deleteIncome(String userId, Long id) {
        var income = additionalIncomeRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Renda não encontrada"));
        additionalIncomeRepository.deleteById(income.id());
    }

    // Recurring income methods

    public List<RecurringIncome> getRecurringIncome(String userId) {
        return recurringIncomeRepository.findByUserId(userId);
    }

    public RecurringIncome saveRecurringIncome(String userId, RecurringIncomeRequest req) {
        var income = new RecurringIncome(null, userId, req.description(), req.amount(), req.dueDay());
        return recurringIncomeRepository.save(income);
    }

    @Transactional
    public void deleteRecurringIncome(String userId, Long id) {
        var template = recurringIncomeRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Renda recorrente não encontrada"));

        // Remove the current-period income this template injected, unless it was edited to a
        // different amount (then keep it as a one-off) — always detach so it survives standalone.
        additionalIncomeRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(i -> id.equals(i.recurringIncomeId()))
                .forEach(i -> {
                    if (i.amount().compareTo(template.amount()) == 0) {
                        additionalIncomeRepository.deleteById(i.id());
                    } else {
                        additionalIncomeRepository.save(new AdditionalIncome(i.id(), i.userId(),
                                i.description(), i.amount(), i.createdAt(), null));
                    }
                });

        recurringIncomeRepository.deleteById(id);
    }

    // Budget limit methods

    public List<BudgetLimit> getBudgetLimits(String userId) {
        return budgetLimitRepository.findByUserId(userId);
    }

    /** Upsert the monthly cap for a category (one limit per user + type). */
    public BudgetLimit saveBudgetLimit(String userId, BudgetLimitRequest req) {
        Long existingId = budgetLimitRepository.findByUserIdAndType(userId, req.type())
                .map(BudgetLimit::id).orElse(null);
        return budgetLimitRepository.save(
                new BudgetLimit(existingId, userId, req.type(), req.monthlyLimit()));
    }

    @Transactional
    public void deleteBudgetLimit(String userId, ExpenseType type) {
        budgetLimitRepository.deleteByUserIdAndType(userId, type);
    }

    private FinanceSettings getOrCreateSettings(String userId) {
        return settingsRepository.findByUserId(userId)
                .orElseGet(() -> settingsRepository.save(new FinanceSettings(userId, 10, null, null)));
    }

    private ExpenseStatus computeStatus(BigDecimal real, BigDecimal estimated) {
        boolean hasReal = real != null && real.compareTo(BigDecimal.ZERO) > 0;
        // No estimate: paid only once something is actually recorded; otherwise still pending.
        if (estimated == null || estimated.compareTo(BigDecimal.ZERO) == 0) {
            return hasReal ? ExpenseStatus.PAID : ExpenseStatus.PENDING;
        }
        int cmp = real.compareTo(estimated);
        if (cmp > 0) return ExpenseStatus.OVERRUN;
        if (cmp == 0) return ExpenseStatus.PAID;
        if (hasReal) return ExpenseStatus.PARTIAL;
        return ExpenseStatus.PENDING;
    }
}
