package tech.lougon.profitly.analysis.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tech.lougon.profitly.analysis.domain.model.DividendEvent;
import tech.lougon.profitly.analysis.domain.model.TickerAnalysis;
import tech.lougon.profitly.analysis.domain.repository.DividendRepository;
import tech.lougon.profitly.analysis.domain.repository.TickerAnalysisRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Read-side aggregation for the advanced stock analysis page.
 * Everything is served from the database — brapi is never called here.
 */
@Service
public class StockAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(StockAnalysisService.class);

    private final JpaStockQuoteRepository quoteRepo;
    private final JpaStockProfileRepository profileRepo;
    private final JpaStockFinancialsRepository financialsRepo;
    private final JpaStockStatementRepository statementRepo;
    private final TickerAnalysisRepository analysisRepository;
    private final DividendRepository dividendRepository;
    private final AnalysisService analysisService;
    private final ObjectMapper objectMapper;

    public StockAnalysisService(JpaStockQuoteRepository quoteRepo,
                                JpaStockProfileRepository profileRepo,
                                JpaStockFinancialsRepository financialsRepo,
                                JpaStockStatementRepository statementRepo,
                                TickerAnalysisRepository analysisRepository,
                                DividendRepository dividendRepository,
                                AnalysisService analysisService,
                                ObjectMapper objectMapper) {
        this.quoteRepo = quoteRepo;
        this.profileRepo = profileRepo;
        this.financialsRepo = financialsRepo;
        this.statementRepo = statementRepo;
        this.analysisRepository = analysisRepository;
        this.dividendRepository = dividendRepository;
        this.analysisService = analysisService;
        this.objectMapper = objectMapper;
    }

    public Optional<StockQuoteJpaEntity> getQuote(String symbol) {
        return quoteRepo.findById(symbol);
    }

    public Optional<StockProfileJpaEntity> getProfile(String symbol) {
        return profileRepo.findById(symbol);
    }

    public Optional<StockFinancialsJpaEntity> getFinancials(String symbol) {
        return financialsRepo.findById(symbol);
    }

    /** Statement rows parsed back to structured JSON, newest first. */
    public List<JsonNode> getStatements(String symbol, String statementType) {
        List<JsonNode> rows = new ArrayList<>();
        for (StockStatementJpaEntity e : statementRepo.findBySymbolAndStatementTypeOrderByEndDateDesc(symbol, statementType)) {
            try {
                rows.add(objectMapper.readTree(e.getRawJson()));
            } catch (Exception ex) {
                log.warn("Failed to parse stored {} for {} ({})", statementType, symbol, e.getEndDate());
            }
        }
        return rows;
    }

    /** Dividend events + annual DY% computed against the average price of each year. */
    public Map<String, Object> getDividendAnalysis(String symbol) {
        List<DividendEvent> events = dividendRepository.findBySymbol(symbol);
        Map<Integer, Double> dyByYear = analysisService.getHistoricalDyByYear(symbol);

        double avgDy5y = dyByYear.entrySet().stream()
                .filter(e -> e.getKey() >= java.time.Year.now().getValue() - 5)
                .mapToDouble(Map.Entry::getValue)
                .average().orElse(0.0);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("events", events);
        result.put("dyByYear", dyByYear);
        result.put("avgDy5y", avgDy5y);
        return result;
    }

    /**
     * Compares the company's fundamental indicators against the average of its sector.
     * Sector comes from stock_profiles; indicators from ticker_analysis + stock_financials.
     */
    public Optional<Map<String, Object>> getSectorComparison(String symbol) {
        StockProfileJpaEntity profile = profileRepo.findById(symbol).orElse(null);
        if (profile == null || profile.getSector() == null) return Optional.empty();

        List<StockProfileJpaEntity> peers = profileRepo.findBySector(profile.getSector());
        List<String> peerSymbols = peers.stream().map(StockProfileJpaEntity::getSymbol).toList();

        Map<String, TickerAnalysis> analyses = new LinkedHashMap<>();
        for (String s : peerSymbols) {
            analysisRepository.findBySymbol(s).ifPresent(a -> analyses.put(s, a));
        }
        Map<String, StockFinancialsJpaEntity> financials = new LinkedHashMap<>();
        for (StockFinancialsJpaEntity f : financialsRepo.findBySymbolIn(peerSymbols)) {
            financials.put(f.getSymbol(), f);
        }

        Map<String, Object> indicators = new LinkedHashMap<>();
        indicators.put("pl", compare(symbol, analyses, a -> toDouble(a.trailingPE())));
        indicators.put("pvp", compare(symbol, analyses, a -> toDouble(a.priceToBook())));
        indicators.put("dividendYield", compare(symbol, analyses, a -> toDouble(a.dividendYield())));
        indicators.put("evEbitda", compare(symbol, analyses, a -> toDouble(a.enterpriseToEbitda())));
        indicators.put("profitMargin", compare(symbol, analyses, a -> toDouble(a.profitMargins())));
        indicators.put("roe", compareFin(symbol, financials, StockFinancialsJpaEntity::getReturnOnEquity));
        indicators.put("roa", compareFin(symbol, financials, StockFinancialsJpaEntity::getReturnOnAssets));
        indicators.put("debtToEquity", compareFin(symbol, financials, StockFinancialsJpaEntity::getDebtToEquity));
        indicators.put("revenueGrowth", compareFin(symbol, financials, StockFinancialsJpaEntity::getRevenueGrowthAnnual));
        indicators.put("grossMargin", compareFin(symbol, financials, StockFinancialsJpaEntity::getGrossMargins));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sector", profile.getSector());
        result.put("industry", profile.getIndustry());
        result.put("peerCount", peerSymbols.size());
        result.put("indicators", indicators);
        return Optional.of(result);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Map<String, Object> compare(String symbol, Map<String, TickerAnalysis> peers,
                                        Function<TickerAnalysis, Double> extractor) {
        Double company = peers.containsKey(symbol) ? extractor.apply(peers.get(symbol)) : null;
        double[] values = peers.values().stream()
                .map(extractor)
                .filter(v -> v != null && !v.isNaN())
                .mapToDouble(Double::doubleValue)
                .toArray();
        return comparisonEntry(company, values);
    }

    private Map<String, Object> compareFin(String symbol, Map<String, StockFinancialsJpaEntity> peers,
                                           Function<StockFinancialsJpaEntity, Double> extractor) {
        Double company = peers.containsKey(symbol) ? extractor.apply(peers.get(symbol)) : null;
        double[] values = peers.values().stream()
                .map(extractor)
                .filter(v -> v != null && !v.isNaN())
                .mapToDouble(Double::doubleValue)
                .toArray();
        return comparisonEntry(company, values);
    }

    private Map<String, Object> comparisonEntry(Double company, double[] sectorValues) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("company", company);
        entry.put("sectorAvg", sectorValues.length > 0
                ? java.util.Arrays.stream(sectorValues).average().orElse(0) : null);
        entry.put("sampleSize", sectorValues.length);
        return entry;
    }

    private static Double toDouble(java.math.BigDecimal v) {
        return v != null ? v.doubleValue() : null;
    }
}
