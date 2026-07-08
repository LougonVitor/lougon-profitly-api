package tech.lougon.profitly.finance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.lougon.profitly.finance.application.dto.CurrentPeriodDTO;
import tech.lougon.profitly.finance.application.dto.ExpenseDTO;
import tech.lougon.profitly.finance.application.service.FinanceService;
import tech.lougon.profitly.finance.domain.model.*;
import tech.lougon.profitly.finance.domain.repository.*;
import tech.lougon.profitly.finance.presentation.request.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the finance budget logic using lightweight in-memory repositories,
 * so the service's real behaviour (status, totals, reset archiving, ownership) is
 * exercised without Spring or a database.
 */
class FinanceServiceTest {

    private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final String USER = "user-1";

    private FinanceService service;
    private InMemoryExpenseRepo expenses;
    private InMemorySettingsRepo settings;
    private InMemoryHistoryRepo history;
    private InMemoryRecurringRepo recurring;
    private InMemoryIncomeRepo incomes;

    @BeforeEach
    void setUp() {
        expenses = new InMemoryExpenseRepo();
        settings = new InMemorySettingsRepo();
        history = new InMemoryHistoryRepo();
        recurring = new InMemoryRecurringRepo();
        incomes = new InMemoryIncomeRepo();
        service = new FinanceService(expenses, settings, history, recurring, incomes);
    }

    // ── computeStatus (via addExpense) ─────────────────────────────────────────

    @Test
    void statusIsPendingWhenNothingRecordedAgainstAnEstimate() {
        ExpenseDTO dto = addExpense("Aluguel", bd(100), bd(0), ExpenseType.HOME);
        assertThat(dto.status()).isEqualTo(ExpenseStatus.PENDING);
    }

    @Test
    void statusIsPartialWhenRealBelowEstimate() {
        assertThat(addExpense("Mercado", bd(200), bd(80), ExpenseType.SUPERMARKET).status())
                .isEqualTo(ExpenseStatus.PARTIAL);
    }

    @Test
    void statusIsPaidWhenRealMeetsEstimate() {
        assertThat(addExpense("Luz", bd(150), bd(150), ExpenseType.HOME).status())
                .isEqualTo(ExpenseStatus.PAID);
    }

    @Test
    void statusIsOverrunWhenRealExceedsEstimate() {
        assertThat(addExpense("Uber", bd(50), bd(75), ExpenseType.LOCOMOTION).status())
                .isEqualTo(ExpenseStatus.OVERRUN);
    }

    @Test
    void statusIsPendingForEmptyExpenseWithoutEstimate() {
        // Regression: previously an expense with no estimate and no value showed "Pago".
        assertThat(addExpense("Avulso", null, bd(0), ExpenseType.SEPARATE).status())
                .isEqualTo(ExpenseStatus.PENDING);
    }

    @Test
    void statusIsPaidWhenSpentWithoutEstimate() {
        assertThat(addExpense("Presente", null, bd(30), ExpenseType.SEPARATE).status())
                .isEqualTo(ExpenseStatus.PAID);
    }

    // ── totals & balance ───────────────────────────────────────────────────────

    @Test
    void currentPeriodComputesIncomeAndBalance() {
        service.updateSettings(USER, new FinanceSettingsRequest(10, bd(5000), null));
        service.addIncome(USER, new AddIncomeRequest("Freela", bd(1000)));
        addExpense("Mercado", bd(400), bd(400), ExpenseType.SUPERMARKET);
        addExpense("Luz", bd(200), bd(150), ExpenseType.HOME);

        CurrentPeriodDTO period = service.getCurrentPeriod(USER);

        assertThat(period.totalIncome()).isEqualByComparingTo(bd(6000));
        assertThat(period.totalReal()).isEqualByComparingTo(bd(550)); // 400 + 150 (+ investment 0)
        assertThat(period.balance()).isEqualByComparingTo(bd(5450));  // 6000 - 550
    }

    @Test
    void getCurrentPeriodCreatesInvestmentRow() {
        CurrentPeriodDTO period = service.getCurrentPeriod(USER);
        assertThat(period.expenses()).anyMatch(e -> e.type() == ExpenseType.INVESTMENT);
    }

    // ── reset / archiving ───────────────────────────────────────────────────────

    @Test
    void resetPeriodArchivesUnderCurrentMonthAndClearsExpenses() {
        addExpense("Mercado", bd(400), bd(400), ExpenseType.SUPERMARKET);
        addExpense("Feira", bd(100), bd(120), ExpenseType.SUPERMARKET);

        service.resetPeriod(USER);

        String thisMonth = YM.format(YearMonth.now());
        assertThat(service.hasPeriodConflict(USER)).isTrue();
        assertThat(history.existsByUserIdAndYearMonth(USER, thisMonth)).isTrue();
        // Two supermarket expenses collapse into one summary row: 400 + 120 = 520
        var summaries = history.findByUserIdAndYearMonthBetween(USER, thisMonth, thisMonth);
        var supermarket = summaries.stream()
                .filter(s -> s.type() == ExpenseType.SUPERMARKET).findFirst().orElseThrow();
        assertThat(supermarket.totalReal()).isEqualByComparingTo(bd(520));
        assertThat(expenses.findByUserId(USER)).isEmpty();
    }

    @Test
    void checkAndResetDoesNothingWhenNotDue() {
        int notToday = java.time.LocalDate.now().getDayOfMonth() == 1 ? 2 : 1;
        service.updateSettings(USER, new FinanceSettingsRequest(notToday, bd(1000), null));
        addExpense("Mercado", bd(400), bd(400), ExpenseType.SUPERMARKET);

        boolean fired = service.checkAndResetIfDue(USER);

        assertThat(fired).isFalse();
        assertThat(expenses.findByUserId(USER)).isNotEmpty();
    }

    // ── recurring template deletion preserves spending ──────────────────────────

    @Test
    void deletingRecurringKeepsExpenseThatHasRecordedSpending() {
        RecurringExpense tmpl = service.saveRecurring(USER,
                new RecurringExpenseRequest("Aluguel", bd(1500), ExpenseType.HOME));
        service.getCurrentPeriod(USER); // auto-populates the linked expense
        Expense generated = expenses.findByUserId(USER).stream()
                .filter(e -> e.title().equals("Aluguel")).findFirst().orElseThrow();
        service.updateExpense(USER, generated.id(),
                new UpdateExpenseRequest(null, null, bd(1500), null, null)); // record payment

        service.deleteRecurring(USER, tmpl.id());

        Expense kept = expenses.findByUserId(USER).stream()
                .filter(e -> e.title().equals("Aluguel")).findFirst().orElseThrow();
        assertThat(kept.realValue()).isEqualByComparingTo(bd(1500));
        assertThat(kept.recurring()).isFalse();          // detached from the template
        assertThat(kept.recurringExpenseId()).isNull();
    }

    @Test
    void deletingRecurringRemovesUntouchedExpense() {
        RecurringExpense tmpl = service.saveRecurring(USER,
                new RecurringExpenseRequest("Netflix", bd(40), ExpenseType.SIGNATURE));
        service.getCurrentPeriod(USER);

        service.deleteRecurring(USER, tmpl.id());

        assertThat(expenses.findByUserId(USER)).noneMatch(e -> e.title().equals("Netflix"));
    }

    // ── ownership / IDOR ────────────────────────────────────────────────────────

    @Test
    void deletingIncomeOfAnotherUserIsRejected() {
        AdditionalIncome mine = service.addIncome(USER, new AddIncomeRequest("Bônus", bd(500)));

        assertThatThrownBy(() -> service.deleteIncome("attacker", mine.id()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(incomes.findByUserIdOrderByCreatedAtDesc(USER)).hasSize(1);
    }

    @Test
    void historyRejectsMalformedMonth() {
        assertThatThrownBy(() -> service.getHistory(USER, "2024-13-oops", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── helpers ─────────────────────────────────────────────────────────────────

    private ExpenseDTO addExpense(String title, BigDecimal est, BigDecimal real, ExpenseType type) {
        return service.addExpense(USER, new AddExpenseRequest(title, est, real, null, type, false));
    }

    private static BigDecimal bd(long v) { return BigDecimal.valueOf(v); }

    // ── in-memory fakes ─────────────────────────────────────────────────────────

    static class InMemoryExpenseRepo implements ExpenseRepository {
        private final Map<Long, Expense> store = new LinkedHashMap<>();
        private final AtomicLong seq = new AtomicLong();

        public Expense save(Expense e) {
            Long id = e.id() != null ? e.id() : seq.incrementAndGet();
            Expense saved = new Expense(id, e.userId(), e.title(), e.estimatedValue(), e.realValue(),
                    e.status(), e.type(), e.createdAt(), e.recurring(), e.recurringExpenseId());
            store.put(id, saved);
            return saved;
        }
        public Optional<Expense> findById(Long id) { return Optional.ofNullable(store.get(id)); }
        public List<Expense> findByUserId(String userId) {
            return store.values().stream().filter(e -> e.userId().equals(userId)).toList();
        }
        public Optional<Expense> findByUserIdAndTitle(String userId, String title) {
            return store.values().stream()
                    .filter(e -> e.userId().equals(userId) && e.title().equalsIgnoreCase(title))
                    .findFirst();
        }
        public void deleteById(Long id) { store.remove(id); }
        public void deleteAllByUserId(String userId) {
            store.values().removeIf(e -> e.userId().equals(userId));
        }
    }

    static class InMemorySettingsRepo implements FinanceSettingsRepository {
        private final Map<String, FinanceSettings> store = new HashMap<>();
        public FinanceSettings save(FinanceSettings s) { store.put(s.userId(), s); return s; }
        public Optional<FinanceSettings> findByUserId(String userId) { return Optional.ofNullable(store.get(userId)); }
        public List<String> findAllUserIds() { return new ArrayList<>(store.keySet()); }
    }

    static class InMemoryHistoryRepo implements ExpenseHistoryRepository {
        private final List<ExpenseHistorySummary> store = new ArrayList<>();
        private final AtomicLong seq = new AtomicLong();
        public void saveAll(List<ExpenseHistorySummary> summaries) {
            for (ExpenseHistorySummary s : summaries) {
                store.add(new ExpenseHistorySummary(seq.incrementAndGet(), s.userId(), s.yearMonth(),
                        s.type(), s.totalReal(), s.totalEstimated()));
            }
        }
        public List<ExpenseHistorySummary> findByUserIdAndYearMonthBetween(String userId, String from, String to) {
            return store.stream()
                    .filter(s -> s.userId().equals(userId)
                            && s.yearMonth().compareTo(from) >= 0 && s.yearMonth().compareTo(to) <= 0)
                    .toList();
        }
        public List<String> findDistinctYearMonthsByUserId(String userId) {
            return store.stream().filter(s -> s.userId().equals(userId))
                    .map(ExpenseHistorySummary::yearMonth).distinct().sorted().collect(Collectors.toList());
        }
        public boolean existsByUserIdAndYearMonth(String userId, String yearMonth) {
            return store.stream().anyMatch(s -> s.userId().equals(userId) && s.yearMonth().equals(yearMonth));
        }
        public void deleteByUserIdAndYearMonth(String userId, String yearMonth) {
            store.removeIf(s -> s.userId().equals(userId) && s.yearMonth().equals(yearMonth));
        }
        public void deleteOlderThan(String userId, String cutoff) {
            store.removeIf(s -> s.userId().equals(userId) && s.yearMonth().compareTo(cutoff) < 0);
        }
    }

    static class InMemoryRecurringRepo implements RecurringExpenseRepository {
        private final Map<Long, RecurringExpense> store = new LinkedHashMap<>();
        private final AtomicLong seq = new AtomicLong();
        public RecurringExpense save(RecurringExpense r) {
            Long id = r.id() != null ? r.id() : seq.incrementAndGet();
            RecurringExpense saved = new RecurringExpense(id, r.userId(), r.title(), r.estimatedValue(), r.type());
            store.put(id, saved);
            return saved;
        }
        public List<RecurringExpense> saveAll(List<RecurringExpense> rs) { return rs.stream().map(this::save).toList(); }
        public List<RecurringExpense> findByUserId(String userId) {
            return store.values().stream().filter(r -> r.userId().equals(userId)).toList();
        }
        public Optional<RecurringExpense> findById(Long id) { return Optional.ofNullable(store.get(id)); }
        public void deleteById(Long id) { store.remove(id); }
    }

    static class InMemoryIncomeRepo implements AdditionalIncomeRepository {
        private final Map<Long, AdditionalIncome> store = new LinkedHashMap<>();
        private final AtomicLong seq = new AtomicLong();
        public AdditionalIncome save(AdditionalIncome a) {
            Long id = a.id() != null ? a.id() : seq.incrementAndGet();
            AdditionalIncome saved = new AdditionalIncome(id, a.userId(), a.description(), a.amount(),
                    a.createdAt() != null ? a.createdAt() : Instant.now());
            store.put(id, saved);
            return saved;
        }
        public List<AdditionalIncome> findByUserIdOrderByCreatedAtDesc(String userId) {
            return store.values().stream().filter(a -> a.userId().equals(userId)).toList();
        }
        public Optional<AdditionalIncome> findByIdAndUserId(Long id, String userId) {
            return Optional.ofNullable(store.get(id)).filter(a -> a.userId().equals(userId));
        }
        public void deleteById(Long id) { store.remove(id); }
    }
}
