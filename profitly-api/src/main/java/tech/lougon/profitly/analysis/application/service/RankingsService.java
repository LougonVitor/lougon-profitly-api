package tech.lougon.profitly.analysis.application.service;

import org.springframework.stereotype.Service;
import tech.lougon.profitly.analysis.application.dto.RankingItemDTO;
import tech.lougon.profitly.analysis.application.dto.RankingsDTO;
import tech.lougon.profitly.analysis.infrastructure.persistence.FiiIndicatorJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaDividendEventRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaFiiIndicatorRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaTickerAnalysisRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.TickerAnalysisJpaEntity;
import tech.lougon.profitly.ticker.infrastructure.persistence.JpaTickerRepository;
import tech.lougon.profitly.ticker.infrastructure.persistence.TickerJpaEntity;

import java.math.BigDecimal;
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
    private final JpaFiiIndicatorRepository fiiIndicatorRepo;

    public RankingsService(JpaTickerRepository tickerRepo,
                           JpaTickerAnalysisRepository analysisRepo,
                           JpaDividendEventRepository dividendRepo,
                           JpaFiiIndicatorRepository fiiIndicatorRepo) {
        this.tickerRepo = tickerRepo;
        this.analysisRepo = analysisRepo;
        this.dividendRepo = dividendRepo;
        this.fiiIndicatorRepo = fiiIndicatorRepo;
    }

    public RankingsDTO getRankings(String assetType) {
        boolean isFii = "FII".equalsIgnoreCase(assetType);

        List<TickerJpaEntity> tickers = assetType == null || assetType.isBlank()
                ? tickerRepo.findAll()
                : tickerRepo.findByAssetTypeIgnoreCase(assetType);

        Map<String, TickerJpaEntity> tickerMap = tickers.stream()
                .collect(Collectors.toMap(TickerJpaEntity::getSymbol, t -> t, (a, b) -> a));

        if (isFii) {
            return buildFiiRankings(tickerMap);
        }

        return buildStockRankings(tickerMap);
    }

    private RankingsDTO buildFiiRankings(Map<String, TickerJpaEntity> tickerMap) {
        Map<String, FiiIndicatorJpaEntity> fiiMap = fiiIndicatorRepo.findAll().stream()
                .filter(f -> tickerMap.containsKey(f.getSymbol()))
                .collect(Collectors.toMap(FiiIndicatorJpaEntity::getSymbol, f -> f, (a, b) -> a));

        // If fii_indicators is still empty (sync not yet run), fall back to ticker_analysis
        if (fiiMap.isEmpty()) {
            return buildFiiFallbackRankings(tickerMap);
        }

        double minEquity = 50_000_000.0;

        List<RankingItemDTO> dividendYield = fiiMap.values().stream()
                .filter(f -> f.getDividendYield12m() != null && f.getDividendYield12m() > 0
                        && f.getDividendYield12m() < 50
                        && f.getEquity() != null && f.getEquity() >= minEquity)
                .sorted(Comparator.comparingDouble(f -> -f.getDividendYield12m()))
                .limit(5)
                .map(f -> {
                    var t = tickerMap.get(f.getSymbol());
                    return new RankingItemDTO(t.getSymbol(), t.getName(), t.getLogoUrl(),
                            f.getDividendYield12m());
                })
                .toList();

        List<RankingItemDTO> marketCap = fiiMap.values().stream()
                .filter(f -> f.getEquity() != null && f.getEquity() >= minEquity)
                .sorted(Comparator.comparingDouble(f -> -f.getEquity()))
                .limit(5)
                .map(f -> {
                    var t = tickerMap.get(f.getSymbol());
                    return new RankingItemDTO(t.getSymbol(), t.getName(), t.getLogoUrl(),
                            f.getEquity());
                })
                .toList();

        List<RankingItemDTO> revenue = fiiMap.values().stream()
                .filter(f -> f.getTotalInvestors() != null && f.getTotalInvestors() > 0
                        && f.getEquity() != null && f.getEquity() >= minEquity)
                .sorted(Comparator.comparingLong(f -> -f.getTotalInvestors()))
                .limit(5)
                .map(f -> {
                    var t = tickerMap.get(f.getSymbol());
                    return new RankingItemDTO(t.getSymbol(), t.getName(), t.getLogoUrl(),
                            f.getTotalInvestors().doubleValue());
                })
                .toList();

        return new RankingsDTO(dividendYield, marketCap, revenue);
    }

    /** Fallback used before the first FII indicator sync completes. Uses ticker_analysis data. */
    private RankingsDTO buildFiiFallbackRankings(Map<String, TickerJpaEntity> tickerMap) {
        Map<String, TickerAnalysisJpaEntity> analysisMap = analysisRepo.findBySymbolIn(tickerMap.keySet()).stream()
                .collect(Collectors.toMap(TickerAnalysisJpaEntity::getSymbol, a -> a, (a, b) -> a));

        List<RankingItemDTO> dividendYield = analysisMap.values().stream()
                .filter(a -> a.getDividendYield() != null
                        && a.getDividendYield().doubleValue() > 0
                        && a.getDividendYield().doubleValue() < 0.50)
                .sorted(Comparator.comparingDouble(a -> -a.getDividendYield().doubleValue()))
                .limit(5)
                .map(a -> {
                    var t = tickerMap.get(a.getSymbol());
                    return new RankingItemDTO(t.getSymbol(), t.getName(), t.getLogoUrl(),
                            a.getDividendYield().doubleValue() * 100); // convert to % for FII display
                })
                .toList();

        // Use bookValue × sharesOutstanding as proxy for equity when fii_indicators not yet synced
        List<RankingItemDTO> marketCap = analysisMap.values().stream()
                .filter(a -> a.getBookValue() != null && a.getSharesOutstanding() != null)
                .sorted(Comparator.comparingDouble(a ->
                        -a.getBookValue().doubleValue() * a.getSharesOutstanding()))
                .limit(5)
                .map(a -> {
                    var t = tickerMap.get(a.getSymbol());
                    double equity = a.getBookValue().doubleValue() * a.getSharesOutstanding();
                    return new RankingItemDTO(t.getSymbol(), t.getName(), t.getLogoUrl(), equity);
                })
                .toList();

        // No totalInvestors available in ticker_analysis — return empty for now
        return new RankingsDTO(dividendYield, marketCap, List.of());
    }

    private RankingsDTO buildStockRankings(Map<String, TickerJpaEntity> tickerMap) {
        Map<String, TickerAnalysisJpaEntity> analysisMap = analysisRepo.findBySymbolIn(tickerMap.keySet()).stream()
                .collect(Collectors.toMap(TickerAnalysisJpaEntity::getSymbol, a -> a, (a, b) -> a));

        long minMarketCap = 500_000_000L;
        long maxMarketCap = 2_000_000_000_000L;

        Map<String, Double> avgDyBySymbol = computeAvgDividendYield(analysisMap, 3);

        List<RankingItemDTO> dividendYield = deduplicateByName(
                analysisMap.values().stream()
                        .filter(a -> avgDyBySymbol.containsKey(a.getSymbol())
                                && a.getMarketCap() != null
                                && a.getMarketCap() >= minMarketCap
                                && a.getMarketCap() <= maxMarketCap)
                        .sorted(Comparator.comparingDouble(a -> -avgDyBySymbol.get(a.getSymbol())))
                        .toList(),
                tickerMap,
                a -> avgDyBySymbol.get(a.getSymbol())
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

    private Map<String, Double> computeAvgDividendYield(
            Map<String, TickerAnalysisJpaEntity> analysisMap, int minYears) {
        List<Object[]> rows = dividendRepo.sumBySymbolAndYear();
        Map<String, Long> yearCountBySymbol = new HashMap<>();
        for (Object[] row : rows) {
            String symbol = (String) row[0];
            yearCountBySymbol.merge(symbol, 1L, Long::sum);
        }
        Map<String, Double> result = new HashMap<>();
        for (Map.Entry<String, Long> entry : yearCountBySymbol.entrySet()) {
            if (entry.getValue() < minYears) continue;
            TickerAnalysisJpaEntity analysis = analysisMap.get(entry.getKey());
            if (analysis == null || analysis.getDividendYield() == null) continue;
            double dy = analysis.getDividendYield().doubleValue();
            if (dy > 0) result.put(entry.getKey(), dy);
        }
        return result;
    }

    private List<RankingItemDTO> deduplicateByName(
            List<TickerAnalysisJpaEntity> sorted,
            Map<String, TickerJpaEntity> tickerMap,
            java.util.function.Function<TickerAnalysisJpaEntity, Double> valueExtractor
    ) {
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
