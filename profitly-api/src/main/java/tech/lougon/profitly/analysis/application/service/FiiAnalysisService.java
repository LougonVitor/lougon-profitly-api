package tech.lougon.profitly.analysis.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tech.lougon.profitly.analysis.application.dto.FiiAnalysisDTO;
import tech.lougon.profitly.analysis.domain.model.PricePoint;
import tech.lougon.profitly.analysis.domain.repository.PriceHistoryRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.FiiDividendEventJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.FiiDocumentJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.FiiIndicatorHistoryJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.FiiIndicatorJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaFiiDividendEventRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaFiiDocumentRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaFiiIndicatorHistoryRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaFiiIndicatorRepository;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Computes advanced FII indicators from the monthly indicator history, dividend
 * events, market price series and raw property/portfolio documents stored by the
 * FII sync. Everything is served from the database — no brapi call.
 */
@Service
public class FiiAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(FiiAnalysisService.class);

    /** Market prices trade on business days, so annualization uses 252 periods. */
    private static final double PERIODS_PER_YEAR = 252.0;

    /**
     * A diversified brick-and-mortar FII effectively never runs above ~40% vacancy; a
     * summary rate at/above this is a bad brapi filing (every property flagged ~100%
     * vacant) and is ignored in favor of the latest sane quarter.
     */
    private static final double IMPLAUSIBLE_VACANCY = 0.60;

    private static final ObjectMapper JSON = new ObjectMapper();

    private final JpaFiiIndicatorRepository fiiRepo;
    private final JpaFiiIndicatorHistoryRepository historyRepo;
    private final JpaFiiDividendEventRepository dividendRepo;
    private final JpaFiiDocumentRepository documentRepo;
    private final PriceHistoryRepository priceHistoryRepository;

    public FiiAnalysisService(JpaFiiIndicatorRepository fiiRepo,
                              JpaFiiIndicatorHistoryRepository historyRepo,
                              JpaFiiDividendEventRepository dividendRepo,
                              JpaFiiDocumentRepository documentRepo,
                              PriceHistoryRepository priceHistoryRepository) {
        this.fiiRepo = fiiRepo;
        this.historyRepo = historyRepo;
        this.dividendRepo = dividendRepo;
        this.documentRepo = documentRepo;
        this.priceHistoryRepository = priceHistoryRepository;
    }

    public Optional<FiiAnalysisDTO> getAnalysis(String symbol) {
        FiiIndicatorJpaEntity fii = fiiRepo.findById(symbol.toUpperCase()).orElse(null);
        if (fii == null) return Optional.empty();

        List<FiiIndicatorHistoryJpaEntity> history =
                historyRepo.findBySymbolOrderByReferenceDateAsc(fii.getSymbol());
        List<FiiDividendEventJpaEntity> dividends =
                dividendRepo.findBySymbolOrderByPaymentDateDesc(fii.getSymbol());
        List<PricePoint> priceHistory = priceHistoryRepository
                .findBySymbolAndDateBetween(fii.getSymbol(), LocalDate.of(2000, 1, 1), LocalDate.now());

        return Optional.of(build(fii, history, dividends, priceHistory));
    }

    private FiiAnalysisDTO build(FiiIndicatorJpaEntity fii,
                                 List<FiiIndicatorHistoryJpaEntity> history,
                                 List<FiiDividendEventJpaEntity> dividends,
                                 List<PricePoint> priceHistory) {
        LocalDate now = LocalDate.now();
        // trailing dividend sums over 3/6/12 months (dividends are payment-date desc)
        String cutoff3m = now.minusMonths(3).toString();
        String cutoff6m = now.minusMonths(6).toString();
        String cutoff12m = now.minusMonths(12).toString();
        double dividendsSum3m = 0, dividendsSum6m = 0, dividendsSum12m = 0;
        int dividendCount12m = 0;
        for (FiiDividendEventJpaEntity d : dividends) {
            if (d.getRate() == null || d.getPaymentDate() == null) continue;
            if (d.getPaymentDate().compareTo(cutoff12m) < 0) break; // desc-ordered
            dividendsSum12m += d.getRate();
            dividendCount12m++;
            if (d.getPaymentDate().compareTo(cutoff6m) >= 0) dividendsSum6m += d.getRate();
            if (d.getPaymentDate().compareTo(cutoff3m) >= 0) dividendsSum3m += d.getRate();
        }

        List<FiiAnalysisDTO.DividendEvent> recentDividends = dividends.stream()
                .map(d -> new FiiAnalysisDTO.DividendEvent(
                        date10(d.getApprovedOn()), date10(d.getLastDatePrior()), date10(d.getPaymentDate()),
                        d.getRate(), d.getLabel()))
                .toList();

        // last payout, magic number (price / last monthly payout), 3m/6m yields on the current price
        Double lastDividend = null, magicNumber = null, dividendYield3m = null, dividendYield6m = null;
        for (FiiDividendEventJpaEntity d : dividends) {
            if (d.getRate() != null && d.getRate() > 0) {
                lastDividend = round4(d.getRate());
                if (fii.getPrice() != null && fii.getPrice() > 0) magicNumber = round2(fii.getPrice() / d.getRate());
                break;
            }
        }
        if (fii.getPrice() != null && fii.getPrice() > 0) {
            if (dividendsSum3m > 0) dividendYield3m = round2(dividendsSum3m / fii.getPrice() * 100.0);
            if (dividendsSum6m > 0) dividendYield6m = round2(dividendsSum6m / fii.getPrice() * 100.0);
        }

        // DY médio 5 anos: the average yearly payout over the last 5 completed years measured
        // against the CURRENT price — "if I buy today, what average yield have the last years
        // paid" (this is how Investidor10 quotes it). Uses each year's total dividends.
        Map<Integer, Double> yearDividends = new LinkedHashMap<>();
        for (FiiDividendEventJpaEntity d : dividends) {
            if (d.getRate() == null || d.getPaymentDate() == null || d.getPaymentDate().length() < 4) continue;
            try {
                yearDividends.merge(Integer.parseInt(d.getPaymentDate().substring(0, 4)), d.getRate(), Double::sum);
            } catch (NumberFormatException ignored) { /* skip malformed year */ }
        }
        Double avgDividendYield = null;
        int currentYear = now.getYear();
        double sum5y = 0;
        int yearsCounted = 0;
        for (int y = currentYear - 5; y < currentYear; y++) {
            Double dv = yearDividends.get(y);
            if (dv != null && dv > 0) { sum5y += dv; yearsCounted++; }
        }
        if (yearsCounted > 0 && fii.getPrice() != null && fii.getPrice() > 0) {
            avgDividendYield = round2((sum5y / yearsCounted) / fii.getPrice() * 100.0);
        }

        // ranking + siblings within the same segment
        Integer dyRank = null, totalInType = null;
        List<FiiAnalysisDTO.SimilarFii> similar = new ArrayList<>();
        if (fii.getSegmentType() != null) {
            List<FiiIndicatorJpaEntity> sameSegment = fiiRepo.findBySegmentTypeIgnoreCase(fii.getSegmentType())
                    .stream()
                    .sorted(Comparator.comparing(FiiIndicatorJpaEntity::getDividendYield12m,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
            totalInType = sameSegment.size();
            for (int i = 0; i < sameSegment.size(); i++) {
                if (fii.getSymbol().equals(sameSegment.get(i).getSymbol())) { dyRank = i + 1; break; }
            }
            sameSegment.stream()
                    .filter(f -> !fii.getSymbol().equals(f.getSymbol()))
                    .forEach(f -> similar.add(new FiiAnalysisDTO.SimilarFii(
                            f.getSymbol(), f.getName(), f.getSegmentType(), f.getPrice(), f.getPriceToNav(),
                            pct(f.getDividendYield12m()), pct(f.getDividendYield1m()),
                            f.getEquity(), f.getTotalInvestors())));
        }

        // market-price-driven indicators (price_points series)
        Map<String, Double> priceReturns = new LinkedHashMap<>();
        Double priceVol1y = null, priceDrawdown1y = null;
        Double price52wHigh = null, price52wLow = null, pricePosition = null;
        List<Bar> priceBars = toPriceBars(priceHistory);
        if (!priceBars.isEmpty()) {
            LocalDate lastPriceDate = priceBars.get(priceBars.size() - 1).date();
            double currentPrice = fii.getPrice() != null
                    ? fii.getPrice() : priceBars.get(priceBars.size() - 1).nav();

            putReturn(priceReturns, priceBars, Bar::nav, currentPrice, lastPriceDate);
            priceVol1y = annualizedVolatility(priceBars, lastPriceDate.minusYears(1));
            priceDrawdown1y = maxDrawdown(priceBars, lastPriceDate.minusYears(1));

            LocalDate cutoff52 = lastPriceDate.minusWeeks(52);
            double hi = -Double.MAX_VALUE, lo = Double.MAX_VALUE;
            for (Bar b : priceBars) {
                if (b.date().isBefore(cutoff52)) continue;
                if (b.nav() > hi) hi = b.nav();
                if (b.nav() < lo) lo = b.nav();
            }
            if (hi > -Double.MAX_VALUE) {
                price52wHigh = hi;
                price52wLow = lo;
                if (hi > lo) pricePosition = clamp((currentPrice - lo) / (hi - lo) * 100.0);
            }
        }

        // average daily financial volume (R$) over the last ~21 trading days
        Double avgDailyLiquidity = averageDailyLiquidity(priceHistory);

        // NAV-history-driven indicators (fii_indicator_history: navPerShare, equity, investors)
        List<Bar> bars = toBars(history);
        int n = bars.size();

        Map<String, Double> navReturns = new LinkedHashMap<>();
        Map<String, Double> equityChanges = new LinkedHashMap<>();
        Map<String, Double> investorsChanges = new LinkedHashMap<>();
        Double nav52wHigh = null, nav52wLow = null, navPosition = null;
        Double navHigh = null, navLow = null;
        String navHighDate = null, navLowDate = null;
        Double vol1y = null, drawdown1y = null;
        Integer historyDays = n > 0 ? n : null;
        String historyStart = n > 0 ? bars.get(0).date().toString() : null;

        if (n > 0) {
            LocalDate lastDate = bars.get(n - 1).date();
            double currentNav = fii.getNavPerShare() != null ? fii.getNavPerShare() : bars.get(n - 1).nav();

            putReturn(navReturns, bars, Bar::nav, currentNav, lastDate);
            Bar last = bars.get(n - 1);
            if (last.equity() != null) putReturn(equityChanges, bars, Bar::equity, last.equity(), lastDate);
            if (last.investors() != null) putReturn(investorsChanges, bars, Bar::investors, last.investors(), lastDate);

            LocalDate cutoff52 = lastDate.minusWeeks(52);
            double hi = -Double.MAX_VALUE, lo = Double.MAX_VALUE;
            for (Bar b : bars) {
                if (b.date().isBefore(cutoff52)) continue;
                if (b.nav() > hi) hi = b.nav();
                if (b.nav() < lo) lo = b.nav();
            }
            if (hi > -Double.MAX_VALUE) {
                nav52wHigh = hi;
                nav52wLow = lo;
                if (hi > lo) navPosition = clamp((currentNav - lo) / (hi - lo) * 100.0);
            }

            for (Bar b : bars) {
                if (navHigh == null || b.nav() > navHigh) { navHigh = b.nav(); navHighDate = b.date().toString(); }
                if (navLow == null || b.nav() < navLow) { navLow = b.nav(); navLowDate = b.date().toString(); }
            }

            vol1y = annualizedVolatility(bars, lastDate.minusYears(1));
            drawdown1y = maxDrawdown(bars, lastDate.minusYears(1));
        }

        Map<String, Object> propertiesDoc = loadDocument(fii.getSymbol(), "properties");
        Map<String, Object> portfolioDoc = loadDocument(fii.getSymbol(), "portfolio");

        // management fee: the monthly report rate is noisy (occasional performance fees),
        // so take the median of the last 12 months and annualize it.
        Double adminFeeRate = medianAdminFee(fii.getSymbol());

        // Vacancy: skip bad brapi filings (implausibly high) and take the most recent sane
        // quarter for both the headline value and the trend.
        Map<String, Double> vacancyHistory = new LinkedHashMap<>();
        Double vacancyRate = null;
        for (FiiDocumentJpaEntity doc : documentRepo.findBySymbolAndDocTypeOrderByReferenceDateAsc(fii.getSymbol(), "properties_history")) {
            try {
                Map<String, Object> parsed = JSON.readValue(doc.getRawJson(), new TypeReference<Map<String, Object>>() {});
                Double raw = numField(mapField(parsed, "summary"), "vacancyRate");
                if (raw == null || raw < 0 || raw >= IMPLAUSIBLE_VACANCY) continue;
                Double asPct = round2(raw * 100.0);
                vacancyHistory.put(doc.getReferenceDate(), asPct);
                vacancyRate = asPct; // ascending order → last assignment is the most recent sane quarter
            } catch (Exception e) {
                log.warn("Failed to parse FII properties_history document {}/{}: {}", fii.getSymbol(), doc.getReferenceDate(), e.getMessage());
            }
        }
        Double currentFilingVacancy = propertiesDoc != null
                ? numField(mapField(propertiesDoc, "summary"), "vacancyRate") : null;
        boolean propertiesFilingPlausible = currentFilingVacancy == null
                || (currentFilingVacancy >= 0 && currentFilingVacancy < IMPLAUSIBLE_VACANCY);
        // no history rows but a plausible current filing → use it directly
        if (vacancyRate == null && currentFilingVacancy != null && propertiesFilingPlausible) {
            vacancyRate = round2(currentFilingVacancy * 100.0);
        }

        // Show the property list even for a bad filing (names/areas/revenue are still useful),
        // but blank out the per-property vacancy when the filing is implausible.
        List<FiiAnalysisDTO.PropertyItem> properties = new ArrayList<>();
        if (propertiesDoc != null) {
            Object rawList = propertiesDoc.get("properties");
            if (rawList instanceof List<?> list) {
                for (Object o : list) {
                    if (!(o instanceof Map<?, ?> m)) continue;
                    @SuppressWarnings("unchecked")
                    Map<String, Object> item = (Map<String, Object>) m;
                    Double propVacancy = propertiesFilingPlausible ? pctField(item, "vacancyRate") : null;
                    properties.add(new FiiAnalysisDTO.PropertyItem(
                            strField(item, "name"), strField(item, "address"), strField(item, "propertyClass"),
                            numField(item, "area"), propVacancy, pctField(item, "revenueShare")));
                }
            }
            properties.sort(Comparator.comparing(FiiAnalysisDTO.PropertyItem::revenueShare,
                    Comparator.nullsLast(Comparator.reverseOrder())));
            if (properties.size() > 15) properties = properties.subList(0, 15);
        }

        List<FiiAnalysisDTO.AllocationItem> allocations = new ArrayList<>();
        if (portfolioDoc != null) {
            Object rawList = portfolioDoc.get("allocations");
            if (rawList instanceof List<?> list) {
                for (Object o : list) {
                    if (!(o instanceof Map<?, ?> m)) continue;
                    @SuppressWarnings("unchecked")
                    Map<String, Object> item = (Map<String, Object>) m;
                    Object countObj = item.get("count");
                    allocations.add(new FiiAnalysisDTO.AllocationItem(
                            strField(item, "assetClass"),
                            countObj instanceof Number num ? num.intValue() : null,
                            numField(item, "value")));
                }
            }
        }

        Map<String, Map<String, Object>> documents = new LinkedHashMap<>();
        if (propertiesDoc != null) documents.put("properties", propertiesDoc);
        if (portfolioDoc != null) documents.put("portfolio", portfolioDoc);

        return new FiiAnalysisDTO(
                fii.getSymbol(), fii.getName(), fii.getCnpj(), fii.getMandate(),
                fii.getSegmentoAtuacao(), fii.getTipoGestao(), fii.getSegmentType(),
                fii.getAdminName(), fii.getAdminCnpj(),
                fii.getPrice(), fii.getNavPerShare(), fii.getPriceToNav(),
                fii.getEquity(), fii.getTotalAssets(), fii.getTotalInvestors(), fii.getSharesOutstanding(),
                fii.getAsOfDate(), fii.getSyncedAt(),
                pct(fii.getMonthlyReturn()),
                pct(fii.getDividendYield12m()), pct(fii.getDividendYield1m()),
                dividendCount12m > 0 ? round4(dividendsSum12m) : null,
                dividendCount12m > 0 ? dividendCount12m : null,
                lastDividend,
                dividendYield3m, dividendYield6m,
                avgDividendYield, avgDailyLiquidity, adminFeeRate,
                magicNumber,
                priceReturns, priceVol1y, priceDrawdown1y,
                price52wHigh, price52wLow, pricePosition,
                navReturns, equityChanges, investorsChanges,
                nav52wHigh, nav52wLow, navPosition,
                navHigh, navHighDate, navLow, navLowDate,
                vol1y, drawdown1y,
                dyRank, totalInType,
                historyDays, historyStart,
                recentDividends, similar,
                vacancyRate, vacancyHistory, properties, allocations,
                documents
        );
    }

    /** Median of the last 12 monthly management-fee rates, annualized (% a.a.) — null if no reports. */
    private Double medianAdminFee(String symbol) {
        List<Double> fees = new ArrayList<>();
        for (FiiDocumentJpaEntity doc : documentRepo.findBySymbolAndDocTypeOrderByReferenceDateAsc(symbol, "report")) {
            try {
                Map<String, Object> parsed = JSON.readValue(doc.getRawJson(), new TypeReference<Map<String, Object>>() {});
                Double fee = numField(parsed, "adminFeeRate");
                if (fee != null && fee > 0) fees.add(fee);
            } catch (Exception e) {
                log.warn("Failed to parse FII report document {}/{}: {}", symbol, doc.getReferenceDate(), e.getMessage());
            }
        }
        if (fees.isEmpty()) return null;
        List<Double> recent = new ArrayList<>(fees.subList(Math.max(0, fees.size() - 12), fees.size()));
        recent.sort(Comparator.naturalOrder());
        int n = recent.size();
        double median = n % 2 == 1 ? recent.get(n / 2) : (recent.get(n / 2 - 1) + recent.get(n / 2)) / 2.0;
        return round4(median * 12.0 * 100.0);
    }

    private Map<String, Object> loadDocument(String symbol, String docType) {
        return documentRepo.findTopBySymbolAndDocTypeOrderByReferenceDateDesc(symbol, docType)
                .map(FiiDocumentJpaEntity::getRawJson)
                .flatMap(raw -> {
                    try {
                        return Optional.of(JSON.readValue(raw, new TypeReference<Map<String, Object>>() {}));
                    } catch (Exception e) {
                        log.warn("Failed to parse stored FII document {}/{}: {}", symbol, docType, e.getMessage());
                        return Optional.empty();
                    }
                })
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapField(Map<String, Object> doc, String key) {
        if (doc == null) return null;
        Object v = doc.get(key);
        return v instanceof Map ? (Map<String, Object>) v : null;
    }

    private static String strField(Map<String, Object> obj, String key) {
        if (obj == null) return null;
        Object v = obj.get(key);
        return v instanceof String s ? s : null;
    }

    private static Double numField(Map<String, Object> obj, String key) {
        if (obj == null) return null;
        Object v = obj.get(key);
        return v instanceof Number num ? num.doubleValue() : null;
    }

    /** Brapi sends vacancy/revenue share fields as decimal ratios (0.0328 = 3.28%) — scaled to percent here. */
    private static Double pctField(Map<String, Object> obj, String key) {
        Double v = numField(obj, key);
        return v != null ? round2(v * 100.0) : null;
    }

    /** Brapi dividend-yield / monthly-return come as decimal fractions (0.12 = 12%) — scaled to percent. */
    private static Double pct(Double fraction) {
        return fraction != null ? round2(fraction * 100.0) : null;
    }

    /** FII dividend dates arrive as "2026-05-29 00:00:00+00" — keep only the yyyy-MM-dd part. */
    private static String date10(String value) {
        return value != null && value.length() >= 10 ? value.substring(0, 10) : value;
    }

    /** One usable history sample: navPerShare plus optional equity and investor count. */
    private record Bar(LocalDate date, double nav, Double equity, Double investors) {}

    /** Average daily financial volume (R$) over the last ~21 trading days: mean of close × share volume. */
    private static Double averageDailyLiquidity(List<PricePoint> priceHistory) {
        double sum = 0;
        int count = 0;
        for (int i = priceHistory.size() - 1; i >= 0 && count < 21; i--) {
            PricePoint p = priceHistory.get(i);
            if (p.close() == null || p.volume() == null) continue;
            double close = p.close().doubleValue();
            if (close <= 0 || p.volume() <= 0) continue;
            sum += close * p.volume();
            count++;
        }
        if (count == 0) return null;
        return (double) Math.round(sum / count);
    }

    /** Market price bars reuse the Bar shape with the close price in the nav slot. */
    private static List<Bar> toPriceBars(List<PricePoint> priceHistory) {
        List<Bar> bars = new ArrayList<>(priceHistory.size());
        for (PricePoint p : priceHistory) {
            if (p.date() == null || p.close() == null) continue;
            double close = p.close().doubleValue();
            if (close <= 0) continue;
            bars.add(new Bar(p.date(), close, null, null));
        }
        return bars;
    }

    private static List<Bar> toBars(List<FiiIndicatorHistoryJpaEntity> history) {
        List<Bar> bars = new ArrayList<>(history.size());
        for (FiiIndicatorHistoryJpaEntity h : history) {
            LocalDate date = parseDate(h.getReferenceDate());
            if (date == null || h.getNavPerShare() == null || h.getNavPerShare() <= 0) continue;
            Double investors = h.getTotalInvestors() != null ? h.getTotalInvestors().doubleValue() : null;
            bars.add(new Bar(date, h.getNavPerShare(), h.getEquity(), investors));
        }
        return bars;
    }

    /** Percent change of {@code metric} from each period start to the current value. */
    private static void putReturn(Map<String, Double> returns, List<Bar> bars,
                                  Function<Bar, Double> metric, double currentValue, LocalDate lastDate) {
        putPeriodReturn(returns, "1m", bars, metric, currentValue, lastDate.minusMonths(1));
        putPeriodReturn(returns, "3m", bars, metric, currentValue, lastDate.minusMonths(3));
        putPeriodReturn(returns, "6m", bars, metric, currentValue, lastDate.minusMonths(6));
        putPeriodReturn(returns, "1y", bars, metric, currentValue, lastDate.minusYears(1));
        putPeriodReturn(returns, "max", bars, metric, currentValue, LocalDate.MIN);
    }

    private static void putPeriodReturn(Map<String, Double> returns, String key, List<Bar> bars,
                                        Function<Bar, Double> metric, double currentValue, LocalDate target) {
        boolean isMax = LocalDate.MIN.equals(target);
        if (!isMax && bars.get(0).date().isAfter(target)) return; // not enough history
        Double base = null;
        for (Bar b : bars) {
            if (!isMax && b.date().isAfter(target)) break;
            Double v = metric.apply(b);
            if (v != null && v > 0) { base = v; if (isMax) break; }
        }
        if (base != null) {
            returns.put(key, round2((currentValue - base) / base * 100.0));
        }
    }

    /** Annualized standard deviation (%) of daily log returns since {@code from}. */
    private static Double annualizedVolatility(List<Bar> bars, LocalDate from) {
        List<Double> values = new ArrayList<>();
        for (Bar b : bars) {
            if (b.date().isBefore(from)) continue;
            values.add(b.nav());
        }
        int count = values.size() - 1;
        if (count < 5) return null; // too few samples to be meaningful

        double mean = 0;
        double[] logReturns = new double[count];
        for (int i = 1; i < values.size(); i++) {
            logReturns[i - 1] = Math.log(values.get(i) / values.get(i - 1));
            mean += logReturns[i - 1];
        }
        mean /= count;
        double var = 0;
        for (double r : logReturns) var += (r - mean) * (r - mean);
        var /= (count - 1);
        return round2(Math.sqrt(var) * Math.sqrt(PERIODS_PER_YEAR) * 100.0);
    }

    /** Worst peak-to-trough decline (%) since {@code from}; negative value. */
    private static Double maxDrawdown(List<Bar> bars, LocalDate from) {
        double peak = 0, worst = 0;
        int samples = 0;
        for (Bar b : bars) {
            if (b.date().isBefore(from)) continue;
            samples++;
            if (b.nav() > peak) peak = b.nav();
            else if (peak > 0) worst = Math.min(worst, (b.nav() - peak) / peak * 100.0);
        }
        return samples >= 5 ? round2(worst) : null;
    }

    private static LocalDate parseDate(String value) {
        if (value == null || value.length() < 10) return null;
        try {
            return LocalDate.parse(value.substring(0, 10));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static double clamp(double v) {
        return Math.max(0.0, Math.min(100.0, v));
    }

    private static Double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static Double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }
}
