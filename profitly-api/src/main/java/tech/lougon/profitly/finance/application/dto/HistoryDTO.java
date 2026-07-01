package tech.lougon.profitly.finance.application.dto;

import tech.lougon.profitly.finance.domain.model.ExpenseHistorySummary;
import tech.lougon.profitly.finance.domain.model.ExpenseType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record HistoryDTO(
        List<MonthSummary> months,
        List<String> availableMonths
) {
    public record MonthSummary(String yearMonth, List<TypeTotal> byType, BigDecimal total) {}
    public record TypeTotal(ExpenseType type, BigDecimal totalReal, BigDecimal totalEstimated) {}

    public static HistoryDTO from(List<ExpenseHistorySummary> summaries, List<String> availableMonths) {
        Map<String, List<ExpenseHistorySummary>> grouped = summaries.stream()
                .collect(Collectors.groupingBy(ExpenseHistorySummary::yearMonth));

        List<MonthSummary> months = grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    List<TypeTotal> byType = entry.getValue().stream()
                            .map(s -> new TypeTotal(s.type(), s.totalReal(), s.totalEstimated()))
                            .toList();
                    BigDecimal total = entry.getValue().stream()
                            .map(ExpenseHistorySummary::totalReal)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new MonthSummary(entry.getKey(), byType, total);
                })
                .toList();

        return new HistoryDTO(months, availableMonths);
    }
}
