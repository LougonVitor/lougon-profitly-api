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
    private static final List<String> ALL_RANGES = List.of("1d", "5d", "1m", "3m", "6m", "1y", "2y", "5y", "10y", "max");

    private final BrapiAnalysisClient client;
    private final JpaIbovespaCacheRepository cacheRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public IbovespaService(BrapiAnalysisClient client,
                           JpaIbovespaCacheRepository cacheRepository) {
        this.client = client;
        this.cacheRepository = cacheRepository;
    }

    public IbovespaResponse fetch(String range) {
        return cacheRepository.findById(range)
                .map(this::fromEntity)
                .orElseGet(() -> new IbovespaResponse(0, 0, 0, 0, List.of()));
    }

    public void syncAll() {
        for (String range : ALL_RANGES) {
            try {
                syncRange(range);
            } catch (Exception e) {
                log.warn("Ibovespa sync failed for range {}: {}", range, e.getMessage());
            }
        }
    }

    public void syncIntraday() {
        try {
            syncRange("1d");
        } catch (Exception e) {
            log.warn("Ibovespa intraday sync failed: {}", e.getMessage());
        }
    }

    private void syncRange(String range) {
        String interval = "1d".equals(range) ? "5m" : "1d";
        List<BrapiHistoricalResponse.PriceBar> raw = client.fetchHistoryWithInterval(IBOV_SYMBOL, range, interval);

        if (raw.isEmpty()) {
            log.warn("Ibovespa: no data returned for range {}", range);
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
            log.error("Failed to serialize Ibovespa points for range {}", range, e);
            return;
        }

        IbovespaCacheEntity entity = cacheRepository.findById(range)
                .orElseGet(() -> new IbovespaCacheEntity(range, 0, 0, 0, 0, "[]", Instant.now()));
        entity.setCurrentPrice(currentPrice);
        entity.setChangePercent(changePercent);
        entity.setPreviousClose(previousClose);
        entity.setOpen(open);
        entity.setPointsJson(pointsJson);
        entity.setSyncedAt(Instant.now());

        cacheRepository.save(entity);
        log.info("Ibovespa cache updated for range={} price={}", range, currentPrice);
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
