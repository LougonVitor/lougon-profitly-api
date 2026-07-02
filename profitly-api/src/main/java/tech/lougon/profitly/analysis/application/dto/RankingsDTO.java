package tech.lougon.profitly.analysis.application.dto;

import java.util.List;

public record RankingsDTO(
        List<RankingItemDTO> dividendYield,
        List<RankingItemDTO> marketCap,
        List<RankingItemDTO> revenue
) {}
