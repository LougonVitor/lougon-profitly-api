package tech.lougon.profitly.analysis.application.dto;

import java.util.List;

public record IbovespaResponse(
        double currentPrice,
        double changePercent,
        double previousClose,
        double open,
        List<PricePoint> points
) {
    public record PricePoint(long date, double close) {}
}
