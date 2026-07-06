package tech.lougon.profitly.analysis.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tech.lougon.profitly.analysis.application.dto.FundAnalysisDTO;
import tech.lougon.profitly.analysis.domain.model.PricePoint;
import tech.lougon.profitly.analysis.domain.repository.PriceHistoryRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.FundDividendEventJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.FundDocumentJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.FundIndicatorJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.FundNavHistoryJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaFundDividendEventRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaFundDocumentRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaFundIndicatorRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaFundNavHistoryRepository;

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
 * Computes advanced fund indicators (FIAGRO, FI-Infra, FIDC, FIP) from the daily
 * NAV series, dividend events and raw documents stored by the fund sync.
 * Everything is served from the database — no brapi call.
 */
@Service
public class FundAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(FundAnalysisService.class);

    /** Funds report NAV on business days, so annualization uses 252 periods. */
    private static final double PERIODS_PER_YEAR = 252.0;

    private static final List<String> DOC_TYPES = List.of(
            "profile", "portfolio", "fiagro_report", "fiagro_portfolio",
            "fidc_report", "fidc_portfolio", "fip_report");

    private static final ObjectMapper JSON = new ObjectMapper();

    private final JpaFundIndicatorRepository fundRepo;
    private final JpaFundNavHistoryRepository navHistoryRepo;
    private final JpaFundDividendEventRepository dividendRepo;
    private final JpaFundDocumentRepository documentRepo;
    private final PriceHistoryRepository priceHistoryRepository;

    public FundAnalysisService(JpaFundIndicatorRepository fundRepo,
                               JpaFundNavHistoryRepository navHistoryRepo,
                               JpaFundDividendEventRepository dividendRepo,
                               JpaFundDocumentRepository documentRepo,
                               PriceHistoryRepository priceHistoryRepository) {
        this.fundRepo = fundRepo;
        this.navHistoryRepo = navHistoryRepo;
        this.dividendRepo = dividendRepo;
        this.documentRepo = documentRepo;
        this.priceHistoryRepository = priceHistoryRepository;
    }

    public Optional<FundAnalysisDTO> getAnalysis(String symbol) {
        FundIndicatorJpaEntity fund = fundRepo.findById(symbol.toUpperCase()).orElse(null);
        if (fund == null) return Optional.empty();

        List<FundNavHistoryJpaEntity> history =
                navHistoryRepo.findBySymbolOrderByReferenceDateAsc(fund.getSymbol());
        List<FundDividendEventJpaEntity> dividends =
                dividendRepo.findBySymbolOrderByPaymentDateDesc(fund.getSymbol());
        List<PricePoint> priceHistory = priceHistoryRepository
                .findBySymbolAndDateBetween(fund.getSymbol(), LocalDate.of(2000, 1, 1), LocalDate.now());

        return Optional.of(build(fund, history, dividends, priceHistory));
    }

    private FundAnalysisDTO build(FundIndicatorJpaEntity fund,
                                  List<FundNavHistoryJpaEntity> history,
                                  List<FundDividendEventJpaEntity> dividends,
                                  List<PricePoint> priceHistory) {
        // dividends summary
        LocalDate now = LocalDate.now();
        String cutoff12m = now.minusMonths(12).toString();
        double dividendsSum12m = 0;
        int dividendCount12m = 0;
        for (FundDividendEventJpaEntity d : dividends) {
            if (d.getRate() == null || d.getPaymentDate() == null) continue;
            if (d.getPaymentDate().compareTo(cutoff12m) < 0) break; // desc-ordered
            dividendsSum12m += d.getRate();
            dividendCount12m++;
        }

        List<FundAnalysisDTO.DividendEvent> recentDividends = dividends.stream()
                .limit(12)
                .map(d -> new FundAnalysisDTO.DividendEvent(
                        d.getDeclaredDate(), d.getLastDatePrior(), d.getPaymentDate(),
                        d.getRate(), d.getLabel()))
                .toList();

        // ranking + siblings within the same fund type
        Integer dyRank = null, totalInType = null;
        List<FundAnalysisDTO.SimilarFund> similar = new ArrayList<>();
        if (fund.getFundType() != null) {
            List<FundIndicatorJpaEntity> sameType = fundRepo.findByFundTypeIgnoreCase(fund.getFundType())
                    .stream()
                    .sorted(Comparator.comparing(FundIndicatorJpaEntity::getDividendYield12m,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
            totalInType = sameType.size();
            for (int i = 0; i < sameType.size(); i++) {
                if (fund.getSymbol().equals(sameType.get(i).getSymbol())) { dyRank = i + 1; break; }
            }
            sameType.stream()
                    .filter(f -> !fund.getSymbol().equals(f.getSymbol()))
                    .forEach(f -> similar.add(new FundAnalysisDTO.SimilarFund(
                            f.getSymbol(), f.getName(), f.getFundType(), f.getPrice(), f.getPriceToNav(),
                            f.getDividendYield12m(), f.getDividendYieldMonthly(),
                            f.getEquity(), f.getTotalInvestors())));
        }

        // market-price-driven indicators (price_points series)
        Map<String, Double> priceReturns = new LinkedHashMap<>();
        Double priceVol1y = null, priceDrawdown1y = null;
        Double price52wHigh = null, price52wLow = null, pricePosition = null;
        List<Bar> priceBars = toPriceBars(priceHistory);
        if (!priceBars.isEmpty()) {
            LocalDate lastPriceDate = priceBars.get(priceBars.size() - 1).date();
            double currentPrice = fund.getPrice() != null
                    ? fund.getPrice() : priceBars.get(priceBars.size() - 1).nav();

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

        // NAV-history-driven indicators
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
            double currentNav = fund.getNavPerShare() != null ? fund.getNavPerShare() : bars.get(n - 1).nav();

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

        return new FundAnalysisDTO(
                fund.getSymbol(), fund.getName(), fund.getLegalName(), fund.getCnpj(),
                fund.getFundType(), fund.getB3Classification(), fund.getIsin(), fund.getStatus(),
                fund.getAdminName(), fund.getManagerName(),
                fund.getPrice(), fund.getNavPerShare(), fund.getPriceToNav(),
                fund.getEquity(), fund.getTotalAssets(), fund.getTotalInvestors(),
                fund.getSharesOutstanding(), fund.getAsOfDate(), fund.getSyncedAt(),
                fund.getMonthlyReturn(), fund.getPatrimonialMonthlyReturn(), fund.getDividendYieldMonthly(),
                fund.getDividendYield12m(), fund.getDividendYield1m(),
                dividendCount12m > 0 ? round4(dividendsSum12m) : null,
                dividendCount12m > 0 ? dividendCount12m : null,
                priceReturns, priceVol1y, priceDrawdown1y,
                price52wHigh, price52wLow, pricePosition,
                navReturns, equityChanges, investorsChanges,
                nav52wHigh, nav52wLow, navPosition,
                navHigh, navHighDate, navLow, navLowDate,
                vol1y, drawdown1y,
                dyRank, totalInType,
                historyDays, historyStart,
                recentDividends, similar,
                loadDocuments(fund.getSymbol())
        );
    }

    /** Latest raw document per type, deserialized back into a map for the response. */
    private Map<String, Map<String, Object>> loadDocuments(String symbol) {
        Map<String, Map<String, Object>> documents = new LinkedHashMap<>();
        for (String docType : DOC_TYPES) {
            documentRepo.findTopBySymbolAndDocTypeOrderByReferenceDateDesc(symbol, docType)
                    .map(FundDocumentJpaEntity::getRawJson)
                    .ifPresent(raw -> {
                        try {
                            documents.put(docType, JSON.readValue(raw, new TypeReference<Map<String, Object>>() {}));
                        } catch (Exception e) {
                            log.warn("Failed to parse stored fund document {}/{}: {}", symbol, docType, e.getMessage());
                        }
                    });
        }
        return documents;
    }

    /** One usable history sample: navPerShare plus optional equity and investor count. */
    private record Bar(LocalDate date, double nav, Double equity, Double investors) {}

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

    private static List<Bar> toBars(List<FundNavHistoryJpaEntity> history) {
        List<Bar> bars = new ArrayList<>(history.size());
        for (FundNavHistoryJpaEntity h : history) {
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

    /** Annualized standard deviation (%) of daily NAV log returns since {@code from}. */
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

    /** Worst peak-to-trough decline (%) of the NAV since {@code from}; negative value. */
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
