package tech.lougon.profitly.analysis.application.service;

import org.springframework.stereotype.Service;
import tech.lougon.profitly.analysis.application.dto.RankingItemDTO;
import tech.lougon.profitly.analysis.application.dto.RankingsDTO;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaTickerAnalysisRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.TickerAnalysisJpaEntity;
import tech.lougon.profitly.ticker.infrastructure.persistence.JpaTickerRepository;
import tech.lougon.profitly.ticker.infrastructure.persistence.TickerJpaEntity;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RankingsService {

    private final JpaTickerRepository tickerRepo;
    private final JpaTickerAnalysisRepository analysisRepo;

    public RankingsService(JpaTickerRepository tickerRepo,
                           JpaTickerAnalysisRepository analysisRepo) {
        this.tickerRepo = tickerRepo;
        this.analysisRepo = analysisRepo;
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

        List<RankingItemDTO> dividendYield = analysisMap.values().stream()
                .filter(a -> a.getDividendYield() != null && a.getDividendYield().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(TickerAnalysisJpaEntity::getDividendYield).reversed())
                .limit(5)
                .map(a -> {
                    var t = tickerMap.get(a.getSymbol());
                    return new RankingItemDTO(t.getSymbol(), t.getName(), t.getLogoUrl(),
                            a.getDividendYield().doubleValue());
                })
                .toList();

        List<RankingItemDTO> marketCap = analysisMap.values().stream()
                .filter(a -> a.getMarketCap() != null && a.getMarketCap() > 0)
                .sorted(Comparator.comparingLong(TickerAnalysisJpaEntity::getMarketCap).reversed())
                .limit(5)
                .map(a -> {
                    var t = tickerMap.get(a.getSymbol());
                    return new RankingItemDTO(t.getSymbol(), t.getName(), t.getLogoUrl(),
                            a.getMarketCap().doubleValue());
                })
                .toList();

        List<RankingItemDTO> revenue = analysisMap.values().stream()
                .filter(a -> a.getEnterpriseValue() != null && a.getEnterpriseToRevenue() != null
                        && a.getEnterpriseToRevenue().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparingDouble(a ->
                        -((TickerAnalysisJpaEntity) a).getEnterpriseValue()
                                / ((TickerAnalysisJpaEntity) a).getEnterpriseToRevenue().doubleValue()))
                .limit(5)
                .map(a -> {
                    var t = tickerMap.get(a.getSymbol());
                    double rev = a.getEnterpriseValue() / a.getEnterpriseToRevenue().doubleValue();
                    return new RankingItemDTO(t.getSymbol(), t.getName(), t.getLogoUrl(), rev);
                })
                .toList();

        return new RankingsDTO(dividendYield, marketCap, revenue);
    }
}
