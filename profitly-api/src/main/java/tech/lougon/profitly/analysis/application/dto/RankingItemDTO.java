package tech.lougon.profitly.analysis.application.dto;

public record RankingItemDTO(
        String symbol,
        String name,
        String logoUrl,
        double value
) {}
