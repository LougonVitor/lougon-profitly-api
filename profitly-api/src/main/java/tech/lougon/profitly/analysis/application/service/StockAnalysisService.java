package tech.lougon.profitly.analysis.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tech.lougon.profitly.analysis.domain.model.DividendEvent;
import tech.lougon.profitly.analysis.domain.model.TickerAnalysis;
import tech.lougon.profitly.analysis.domain.repository.DividendRepository;
import tech.lougon.profitly.analysis.domain.repository.TickerAnalysisRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.*;

import java.time.LocalDate;

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
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StockAnalysisService(JpaStockQuoteRepository quoteRepo,
                                JpaStockProfileRepository profileRepo,
                                JpaStockFinancialsRepository financialsRepo,
                                JpaStockStatementRepository statementRepo,
                                TickerAnalysisRepository analysisRepository,
                                DividendRepository dividendRepository,
                                AnalysisService analysisService) {
        this.quoteRepo = quoteRepo;
        this.profileRepo = profileRepo;
        this.financialsRepo = financialsRepo;
        this.statementRepo = statementRepo;
        this.analysisRepository = analysisRepository;
        this.dividendRepository = dividendRepository;
        this.analysisService = analysisService;
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
    public List<Map<String, Object>> getStatements(String symbol, String statementType) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (StockStatementJpaEntity e : statementRepo.findBySymbolAndStatementTypeOrderByEndDateDesc(symbol, statementType)) {
            try {
                rows.add(objectMapper.readValue(e.getRawJson(), new TypeReference<Map<String, Object>>() {}));
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
        Map<String, StockQuoteJpaEntity> quotes = new LinkedHashMap<>();
        for (StockQuoteJpaEntity q : quoteRepo.findAllById(peerSymbols)) {
            quotes.put(q.getSymbol(), q);
        }

        // Effective P/L: brapi value when present, otherwise price / EPS (same fallback the
        // metrics bar uses). Sector average ignores negative P/L (loss-making companies).
        Map<String, Double> effectivePl = new LinkedHashMap<>();
        for (Map.Entry<String, TickerAnalysis> en : analyses.entrySet()) {
            Double v = toDouble(en.getValue().trailingPE());
            if (v == null) {
                Double eps = toDouble(en.getValue().earningsPerShare());
                StockQuoteJpaEntity q = quotes.get(en.getKey());
                Double price = q != null ? q.getPrice() : null;
                if (eps != null && eps != 0 && price != null) v = price / eps;
            }
            if (v != null) effectivePl.put(en.getKey(), v);
        }

        Map<String, Object> indicators = new LinkedHashMap<>();
        indicators.put("pl", comparisonEntry(effectivePl.get(symbol),
                effectivePl.values().stream().filter(v -> v > 0).mapToDouble(Double::doubleValue).toArray()));
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

    /**
     * Computes the Investidor10-style fundamental indicator grid from stored data only.
     * Percentages are returned as fractions (0.0823 = 8.23%); ratios as plain numbers.
     * Indicators whose inputs are missing come back as null and are hidden by the UI.
     */
    public Map<String, Double> getKeyIndicators(String symbol) {
        TickerAnalysis a = analysisRepository.findBySymbol(symbol).orElse(null);
        StockFinancialsJpaEntity f = financialsRepo.findById(symbol).orElse(null);
        StockQuoteJpaEntity q = quoteRepo.findById(symbol).orElse(null);

        Double price = q != null ? q.getPrice() : null;
        // no nested ternaries here — mixing primitive double with a nullable Double branch
        // makes Java unbox the null and NPE
        Double marketCap = null;
        if (a != null && a.marketCap() != null) marketCap = a.marketCap().doubleValue();
        else if (q != null && q.getMarketCap() != null) marketCap = q.getMarketCap().doubleValue();
        Double enterpriseValue = a != null && a.enterpriseValue() != null ? a.enterpriseValue().doubleValue() : null;
        Double eps = a != null ? toDouble(a.earningsPerShare()) : null;
        Double totalRevenueTtm = f != null && f.getTotalRevenue() != null ? f.getTotalRevenue().doubleValue() : null;

        // Latest balance sheet (any period — quarterly is the freshest snapshot)
        Map<String, Object> balance = latestStatement(symbol, "balance_sheet", null);
        Double totalAssets = numOf(balance, "totalAssets");
        Double equity = numOf(balance, "shareholdersEquity", "totalStockholderEquity");
        Double totalLiab = numOf(balance, "totalLiab");
        if (totalLiab == null && totalAssets != null && equity != null) totalLiab = totalAssets - equity;
        Double curAssets = numOf(balance, "totalCurrentAssets", "currentAssets");
        Double curLiab = numOf(balance, "currentLiabilities", "totalCurrentLiabilities");

        // Latest yearly income statement for EBIT / NOPAT
        Map<String, Object> income = latestStatement(symbol, "income_statement", "yearly");
        Double ebit = numOf(income, "ebit", "cleanEbit");
        Double nopat = numOf(income, "cleanNopat");
        Double revenueYearly = numOf(income, "totalRevenue");

        // Dividends of the last 12 months (per share)
        double div12m = 0;
        String cutoff = LocalDate.now().minusMonths(12).toString();
        for (DividendEvent d : dividendRepository.findBySymbol(symbol)) {
            if (d.rate() == null || d.rate() <= 0 || d.lastDatePrior() == null || d.lastDatePrior().length() < 10) continue;
            if (d.lastDatePrior().substring(0, 10).compareTo(cutoff) >= 0) div12m += d.rate();
        }

        Double pl = a != null ? toDouble(a.trailingPE()) : null;
        if (pl == null && eps != null && eps != 0 && price != null) pl = price / eps;

        Double dy = a != null ? toDouble(a.dividendYield()) : null;
        if (dy == null && price != null && price > 0 && div12m > 0) dy = div12m / price;

        Map<String, Double> ind = new LinkedHashMap<>();
        ind.put("pl", pl);
        ind.put("psr", ratio(marketCap, totalRevenueTtm != null ? totalRevenueTtm : revenueYearly));
        ind.put("pvp", a != null ? toDouble(a.priceToBook()) : null);
        ind.put("dividendYield", dy);
        ind.put("payout", eps != null && eps > 0 && div12m > 0 ? div12m / eps : null);
        ind.put("margemLiquida", f != null ? f.getProfitMargins() : null);
        ind.put("margemBruta", f != null ? f.getGrossMargins() : null);
        Double margemEbit = null;
        if (ebit != null && revenueYearly != null && revenueYearly != 0) margemEbit = ebit / revenueYearly;
        else if (f != null) margemEbit = f.getOperatingMargins();
        ind.put("margemEbit", margemEbit);
        ind.put("evEbit", ratio(enterpriseValue, ebit));
        ind.put("pEbit", ratio(marketCap, ebit));
        ind.put("pAtivo", ratio(marketCap, totalAssets));
        ind.put("pCapGiro", curAssets != null && curLiab != null
                ? ratio(marketCap, curAssets - curLiab) : null);
        ind.put("pAtivoCircLiq", curAssets != null && totalLiab != null
                ? ratio(marketCap, curAssets - totalLiab) : null);
        ind.put("vpa", a != null ? toDouble(a.bookValue()) : null);
        ind.put("lpa", eps);
        ind.put("beta", a != null ? toDouble(a.beta()) : null);
        ind.put("pegRatio", a != null ? toDouble(a.pegRatio()) : null);
        ind.put("giroAtivos", ratio(totalRevenueTtm != null ? totalRevenueTtm : revenueYearly, totalAssets));
        ind.put("roe", f != null ? f.getReturnOnEquity() : null);
        ind.put("roic", computeRoic(nopat, ebit, equity, f));
        ind.put("roa", f != null ? f.getReturnOnAssets() : null);
        ind.put("patrimonioAtivos", ratio(equity, totalAssets));
        ind.put("passivosAtivos", ratio(totalLiab, totalAssets));
        ind.put("liquidezCorrente", f != null ? f.getCurrentRatio() : null);
        ind.put("cagrReceitas5a", cagr5y(symbol, "totalRevenue"));
        ind.put("cagrLucros5a", cagr5y(symbol, "netIncome", "netIncomeFromContinuingOps", "netIncomeApplicableToCommonShares"));
        return ind;
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Double computeRoic(Double nopat, Double ebit, Double equity, StockFinancialsJpaEntity f) {
        // NOPAT: reported when available, otherwise EBIT net of the standard 34% corporate tax
        Double effectiveNopat = nopat != null ? nopat : (ebit != null ? ebit * 0.66 : null);
        if (effectiveNopat == null || equity == null || f == null) return null;
        double investedCapital = equity
                + (f.getTotalDebt() != null ? f.getTotalDebt() : 0)
                - (f.getTotalCash() != null ? f.getTotalCash() : 0);
        return investedCapital != 0 ? effectiveNopat / investedCapital : null;
    }

    /** 5-year CAGR from yearly income statements: (latest / 5-years-ago)^(1/years) − 1. */
    private Double cagr5y(String symbol, String... fields) {
        List<Map<String, Object>> yearly = getStatements(symbol, "income_statement").stream()
                .filter(r -> "yearly".equalsIgnoreCase(String.valueOf(r.get("type"))))
                .toList(); // already sorted newest first
        if (yearly.size() < 2) return null;

        Map<String, Object> latest = yearly.get(0);
        Double v1 = numOf(latest, fields);
        int latestYear = yearOf(latest);
        if (v1 == null || v1 <= 0 || latestYear == 0) return null;

        // find the row closest to 5 years before the latest
        Map<String, Object> base = null;
        int baseYear = 0;
        for (Map<String, Object> row : yearly) {
            int y = yearOf(row);
            if (y != 0 && y <= latestYear - 5) { base = row; baseYear = y; break; }
        }
        if (base == null) { base = yearly.get(yearly.size() - 1); baseYear = yearOf(base); }
        int years = latestYear - baseYear;
        Double v0 = numOf(base, fields);
        if (v0 == null || v0 <= 0 || years < 2) return null;

        return Math.pow(v1 / v0, 1.0 / years) - 1.0;
    }

    private Map<String, Object> latestStatement(String symbol, String statementType, String periodType) {
        return getStatements(symbol, statementType).stream()
                .filter(r -> periodType == null || periodType.equalsIgnoreCase(String.valueOf(r.get("type"))))
                .findFirst()
                .orElse(Map.of());
    }

    private static int yearOf(Map<String, Object> row) {
        Object endDate = row.get("endDate");
        if (endDate == null || String.valueOf(endDate).length() < 4) return 0;
        try {
            return Integer.parseInt(String.valueOf(endDate).substring(0, 4));
        } catch (NumberFormatException e) { return 0; }
    }

    private static Double numOf(Map<String, Object> row, String... fields) {
        for (String field : fields) {
            Object v = row.get(field);
            if (v instanceof Number n) return n.doubleValue();
        }
        return null;
    }

    private static Double ratio(Double numerator, Double denominator) {
        if (numerator == null || denominator == null || denominator == 0) return null;
        return numerator / denominator;
    }

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
