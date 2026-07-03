package tech.lougon.profitly.analysis.application.service;

import org.springframework.stereotype.Service;
import tech.lougon.profitly.analysis.application.dto.RankingItemDTO;
import tech.lougon.profitly.analysis.application.dto.RankingsDTO;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaDividendEventRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaTickerAnalysisRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.TickerAnalysisJpaEntity;
import tech.lougon.profitly.ticker.infrastructure.persistence.JpaTickerRepository;
import tech.lougon.profitly.ticker.infrastructure.persistence.TickerJpaEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RankingsService {

    private final JpaTickerRepository tickerRepo;
    private final JpaTickerAnalysisRepository analysisRepo;
    private final JpaDividendEventRepository dividendRepo;

    public RankingsService(JpaTickerRepository tickerRepo,
                           JpaTickerAnalysisRepository analysisRepo,
                           JpaDividendEventRepository dividendRepo) {
        this.tickerRepo = tickerRepo;
        this.analysisRepo = analysisRepo;
        this.dividendRepo = dividendRepo;
    }

    public RankingsDTO getRankings(String assetType) {
        List<TickerJpaEntity> tickers = assetType == null || assetType.isBlank()
                ? tickerRepo.findAll()
                : tickerRepo.findByAssetTypeIgnoreCase(assetType);

        Map<String, TickerJpaEntity> tickerMap = tickers.stream()
                .collect(Collectors.toMap(TickerJpaEntity::getSymbol, t -> t, (a, b) -> a));

        Map<String, TickerAnalysisJpaEntity> analysisMap = analysisRepo.findAll().stream()
                .filter(a -> tickerMap.containsKey(a.getSymbol()))
                .collect(Collectors.toMap(TickerAnalysisJpaEntity::getSymbol, a -> a, (a, b) -> a));

        // Liquidity floor: ignore micro-caps with no real market presence.
        long minMarketCap = 500_000_000L;         // R$ 500 million
        // Sanity ceiling: above R$ 2 trillion is a brapi data error.
        long maxMarketCap = 2_000_000_000_000L;

        // Average annual dividend per share: sum dividends per year, average across years.
        // Minimum 3 years of payment history required to appear.
        Map<String, Double> avgAnnualDiv = computeAvgAnnualDividend(3);

        // Build a synthetic list sorted by avgAnnualDiv for deduplication
        List<TickerAnalysisJpaEntity> sortedByAvgDiv = analysisMap.values().stream()
                .filter(a -> avgAnnualDiv.containsKey(a.getSymbol())
                        && avgAnnualDiv.get(a.getSymbol()) > 0
                        && a.getMarketCap() != null
                        && a.getMarketCap() >= minMarketCap
                        && a.getMarketCap() <= maxMarketCap)
                .sorted(Comparator.comparingDouble(a -> -avgAnnualDiv.get(a.getSymbol())))
                .toList();

        List<RankingItemDTO> dividendYield = deduplicateByName(
                sortedByAvgDiv,
                tickerMap,
                a -> avgAnnualDiv.getOrDefault(a.getSymbol(), 0.0)
        );

        List<RankingItemDTO> marketCap = deduplicateByName(
                analysisMap.values().stream()
                        .filter(a -> a.getMarketCap() != null
                                && a.getMarketCap() >= minMarketCap
                                && a.getMarketCap() <= maxMarketCap)
                        .sorted(Comparator.comparingLong(TickerAnalysisJpaEntity::getMarketCap).reversed())
                        .toList(),
                tickerMap,
                a -> a.getMarketCap().doubleValue()
        );

        List<RankingItemDTO> revenue = deduplicateByName(
                analysisMap.values().stream()
                        .filter(a -> a.getEnterpriseValue() != null && a.getEnterpriseToRevenue() != null
                                && a.getEnterpriseToRevenue().compareTo(BigDecimal.ZERO) > 0
                                && a.getMarketCap() != null
                                && a.getMarketCap() >= minMarketCap
                                && a.getMarketCap() <= maxMarketCap)
                        .sorted(Comparator.comparingDouble(a ->
                                -((TickerAnalysisJpaEntity) a).getEnterpriseValue()
                                        / ((TickerAnalysisJpaEntity) a).getEnterpriseToRevenue().doubleValue()))
                        .toList(),
                tickerMap,
                a -> a.getEnterpriseValue() / a.getEnterpriseToRevenue().doubleValue()
        );

        return new RankingsDTO(dividendYield, marketCap, revenue);
    }

    private Map<String, Double> computeAvgAnnualDividend(int minYears) {
        // sumBySymbolAndYear returns [symbol, year, sum] rows
        List<Object[]> rows = dividendRepo.sumBySymbolAndYear();

        // Accumulate per (symbol → year → total)
        Map<String, Map<String, Double>> bySymbolYear = new HashMap<>();
        for (Object[] row : rows) {
            String symbol = (String) row[0];
            String year   = (String) row[1];
            double total  = ((Number) row[2]).doubleValue();
            bySymbolYear.computeIfAbsent(symbol, k -> new HashMap<>()).put(year, total);
        }

        // Average the per-year totals; require at least minYears of history
        Map<String, Double> result = new HashMap<>();
        for (Map.Entry<String, Map<String, Double>> entry : bySymbolYear.entrySet()) {
            Map<String, Double> yearTotals = entry.getValue();
            if (yearTotals.size() < minYears) continue;
            double avg = yearTotals.values().stream()
                    .mapToDouble(Double::doubleValue).average().orElse(0);
            if (avg > 0) result.put(entry.getKey(), avg);
        }
        return result;
    }

    private List<RankingItemDTO> deduplicateByName(
            List<TickerAnalysisJpaEntity> sorted,
            Map<String, TickerJpaEntity> tickerMap,
            java.util.function.Function<TickerAnalysisJpaEntity, Double> valueExtractor
    ) {
        // Deduplicate by ticker prefix (strip trailing digits: PETR3/PETR4 → PETR).
        // List is pre-sorted descending so first occurrence is always the best value.
        java.util.Set<String> seenPrefixes = new java.util.LinkedHashSet<>();
        return sorted.stream()
                .filter(a -> {
                    if (tickerMap.get(a.getSymbol()) == null) return false;
                    String prefix = a.getSymbol().replaceAll("\\d+$", "").toUpperCase();
                    return seenPrefixes.add(prefix);
                })
                .limit(5)
                .map(a -> {
                    var t = tickerMap.get(a.getSymbol());
                    return new RankingItemDTO(t.getSymbol(), t.getName(), t.getLogoUrl(),
                            valueExtractor.apply(a));
                })
                .toList();
    }
}
