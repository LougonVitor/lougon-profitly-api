package tech.lougon.profitly.analysis.application.service;

import org.springframework.stereotype.Service;
import tech.lougon.profitly.analysis.application.dto.IbovespaResponse;
import tech.lougon.profitly.analysis.infrastructure.client.BrapiAnalysisClient;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiHistoricalResponse;

import java.util.List;

@Service
public class IbovespaService {

    private static final String IBOV_SYMBOL = "^BVSP";

    private final BrapiAnalysisClient client;

    public IbovespaService(BrapiAnalysisClient client) {
        this.client = client;
    }

    public IbovespaResponse fetch(String range) {
        String interval = "1d".equals(range) ? "5m" : "1d";
        List<BrapiHistoricalResponse.PriceBar> bars = client.fetchHistoryWithInterval(IBOV_SYMBOL, range, interval);

        if (bars.isEmpty()) {
            return new IbovespaResponse(0, 0, 0, 0, List.of());
        }

        var last = bars.get(bars.size() - 1);
        var first = bars.get(0);

        double currentPrice = last.close() != null ? last.close() : 0;
        double open = first.open() != null ? first.open() : 0;
        double previousClose = open > 0 ? open : currentPrice;
        double changePercent = previousClose > 0
                ? ((currentPrice - previousClose) / previousClose) * 100
                : 0;

        List<IbovespaResponse.PricePoint> points = bars.stream()
                .filter(b -> b.date() != null && b.close() != null)
                .map(b -> new IbovespaResponse.PricePoint(b.date(), b.close()))
                .toList();

        return new IbovespaResponse(currentPrice, changePercent, previousClose, open, points);
    }
}
