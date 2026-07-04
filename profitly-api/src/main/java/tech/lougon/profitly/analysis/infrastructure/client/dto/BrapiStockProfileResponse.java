package tech.lougon.profitly.analysis.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** Response of /api/v2/stocks/profile?symbols=... */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BrapiStockProfileResponse(List<Result> results) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Result(String symbol, Data data) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(
            String address1,
            String address2,
            String city,
            String state,
            String zip,
            String country,
            String phone,
            String website,
            String industry,
            String industryKey,
            String sector,
            String sectorKey,
            String longBusinessSummary,
            Long fullTimeEmployees,
            String twitter,
            String name,
            String startDate,
            String logoUrl,
            String cnpj
    ) {}
}
