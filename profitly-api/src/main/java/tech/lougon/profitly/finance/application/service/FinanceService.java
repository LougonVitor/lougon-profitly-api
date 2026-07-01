package tech.lougon.profitly.finance.application.service;

import org.springframework.stereotype.Service;
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

    public FinanceService(ExpenseRepository expenseRepository,
                          FinanceSettingsRepository settingsRepository,
                          ExpenseHistoryRepository historyRepository) {
        this.expenseRepository = expenseRepository;
        this.settingsRepository = settingsRepository;
        this.historyRepository = historyRepository;
    }

    public CurrentPeriodDTO getCurrentPeriod(String userId) {
        var settings = getOrCreateSettings(userId);
        var expenses = expenseRepository.findByUserId(userId);
        return CurrentPeriodDTO.from(expenses, settings);
    }

    public ExpenseDTO addExpense(String userId, AddExpenseRequest req) {
        var expense = new Expense(null, userId, req.title(),
                req.estimatedValue(), req.realValue() != null ? req.realValue() : BigDecimal.ZERO,
                req.status() != null ? req.status() : ExpenseStatus.PENDING,
                req.type(), Instant.now());
        return ExpenseDTO.from(expenseRepository.save(expense));
    }

    public ExpenseDTO quickLaunch(String userId, QuickLaunchRequest req) {
        var existing = expenseRepository.findByUserIdAndTitle(userId, req.title())
                .orElseThrow(() -> new IllegalArgumentException("Lançamento não encontrado: " + req.title()));

        BigDecimal newReal = existing.realValue().add(req.value());
        BigDecimal estimated = existing.estimatedValue();
        ExpenseStatus status = computeStatus(newReal, estimated);

        var updated = new Expense(existing.id(), existing.userId(), existing.title(),
                estimated, newReal, status, existing.type(), existing.createdAt());
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
                existing.createdAt());
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
