package tech.lougon.profitly.analysis.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tech.lougon.profitly.analysis.application.dto.IbovespaResponse;
import tech.lougon.profitly.analysis.infrastructure.client.BrapiAnalysisClient;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiHistoricalResponse;
import tech.lougon.profitly.analysis.infrastructure.persistence.IbovespaCacheEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaIbovespaCacheRepository;

import java.time.Instant;
import java.util.List;

@Service
public class IbovespaService {

    private static final Logger log = LoggerFactory.getLogger(IbovespaService.class);
    private static final String IBOV_SYMBOL = "^BVSP";
    // Benchmark indices served from this cache. IBOV is the Dashboard widget and the
    // stock "vs IBOV" chart; IFIX is the FII "vs IFIX" chart. brapi historical symbols.
    private static final java.util.Map<String, String> INDEX_SYMBOLS = java.util.Map.of(
            "ibov", IBOV_SYMBOL,
            "ifix", "IFIX.SA");
    // Cache keys are brapi ranges (Yahoo-style suffixes). Covers both the Dashboard
    // IBOV widget (1d/5d/1mo/6mo/1y/5y) and the ticker "vs IBOV" chart (which uses
    // the same 1m/3m/6m/1y/2y/5y/10y/max vocabulary as stock price history —
    // normalized to these keys in fetch(), see RANGE_ALIASES).
    private static final List<String> ALL_RANGES =
            List.of("1d", "5d", "1mo", "3mo", "6mo", "1y", "2y", "5y", "10y", "max");
    // Maps the ticker page's stock-history-style range values to the brapi/cache keys above.
    private static final java.util.Map<String, String> RANGE_ALIASES =
            java.util.Map.of("1m", "1mo", "3m", "3mo", "6m", "6mo");

    private final BrapiAnalysisClient client;
    private final JpaIbovespaCacheRepository cacheRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public IbovespaService(BrapiAnalysisClient client,
                           JpaIbovespaCacheRepository cacheRepository) {
        this.client = client;
        this.cacheRepository = cacheRepository;
    }

    public IbovespaResponse fetch(String range) {
        return fetch(range, "ibov");
    }

    public IbovespaResponse fetch(String range, String index) {
        String key = cacheKey(index, RANGE_ALIASES.getOrDefault(range, range));
        return cacheRepository.findById(key)
                .map(this::fromEntity)
                .orElseGet(() -> new IbovespaResponse(0, 0, 0, 0, List.of()));
    }

    public void syncAll() {
        for (String index : INDEX_SYMBOLS.keySet()) {
            for (String range : ALL_RANGES) {
                try {
                    syncRange(index, range);
                } catch (Exception e) {
                    log.warn("{} sync failed for range {}: {}", index, range, e.getMessage());
                }
            }
        }
    }

    public void syncIntraday() {
        try {
            syncRange("ibov", "1d");
        } catch (Exception e) {
            log.warn("Ibovespa intraday sync failed: {}", e.getMessage());
        }
    }

    // IBOV rows keep their bare range as PK (pre-existing cache); other indices are namespaced.
    private String cacheKey(String index, String range) {
        return "ibov".equals(index) ? range : index + ":" + range;
    }

    private void syncRange(String index, String range) {
        String symbol = INDEX_SYMBOLS.getOrDefault(index, IBOV_SYMBOL);
        String interval = "1d".equals(range) ? "5m" : "1d";
        List<BrapiHistoricalResponse.PriceBar> raw = client.fetchHistoryWithInterval(symbol, range, interval);

        if (raw.isEmpty()) {
            log.warn("{}: no data returned for range {}", index, range);
            return;
        }

        List<BrapiHistoricalResponse.PriceBar> bars = raw.stream()
                .filter(b -> b.date() != null && b.close() != null)
                .sorted(java.util.Comparator.comparingLong(BrapiHistoricalResponse.PriceBar::date))
                .toList();

        if (bars.isEmpty()) return;

        var last = bars.get(bars.size() - 1);
        var first = bars.get(0);

        double currentPrice = last.close();
        double open = first.open() != null ? first.open() : 0;
        double previousClose = open > 0 ? open : currentPrice;
        double changePercent = previousClose > 0
                ? ((currentPrice - previousClose) / previousClose) * 100
                : 0;

        List<IbovespaResponse.PricePoint> points = bars.stream()
                .map(b -> new IbovespaResponse.PricePoint(b.date(), b.close()))
                .toList();

        String pointsJson;
        try {
            pointsJson = objectMapper.writeValueAsString(points);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize {} points for range {}", index, range, e);
            return;
        }

        String key = cacheKey(index, range);
        IbovespaCacheEntity entity = cacheRepository.findById(key)
                .orElseGet(() -> new IbovespaCacheEntity(key, 0, 0, 0, 0, "[]", Instant.now()));
        entity.setCurrentPrice(currentPrice);
        entity.setChangePercent(changePercent);
        entity.setPreviousClose(previousClose);
        entity.setOpen(open);
        entity.setPointsJson(pointsJson);
        entity.setSyncedAt(Instant.now());

        cacheRepository.save(entity);
        log.info("{} cache updated for range={} price={}", index, range, currentPrice);
    }

    private IbovespaResponse fromEntity(IbovespaCacheEntity e) {
        List<IbovespaResponse.PricePoint> points;
        try {
            points = objectMapper.readValue(e.getPointsJson(),
                    new TypeReference<List<IbovespaResponse.PricePoint>>() {});
        } catch (JsonProcessingException ex) {
            log.error("Failed to deserialize Ibovespa points for range {}", e.getRange(), ex);
            points = List.of();
        }
        return new IbovespaResponse(e.getCurrentPrice(), e.getChangePercent(),
                e.getPreviousClose(), e.getOpen(), points);
    }
}
