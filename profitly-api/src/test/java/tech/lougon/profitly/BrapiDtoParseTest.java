package tech.lougon.profitly;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.lougon.profitly.analysis.infrastructure.client.dto.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the brapi API JSON response structures are correctly mapped to DTOs.
 * Uses real-world sample payloads as documented by brapi.
 */
class BrapiDtoParseTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    // ── FII List ─────────────────────────────────────────────────────────────

    @Test
    void fiiList_parsesSymbolPriceAndIndicators() throws Exception {
        String json = """
                {
                  "fiis": [
                    {
                      "symbol": "MXRF11",
                      "name": "Maxi Renda FII",
                      "price": 10.52,
                      "navPerShare": 10.80,
                      "priceToNav": 0.974,
                      "dividendYield12m": 12.5,
                      "totalInvestors": 850000,
                      "segmentType": "papel",
                      "administratorName": "BTG Pactual",
                      "administratorCnpj": "30.822.936/0001-88"
                    }
                  ]
                }
                """;

        BrapiFiiListResponse response = mapper.readValue(json, BrapiFiiListResponse.class);

        assertThat(response).isNotNull();
        assertThat(response.fiis()).hasSize(1);

        BrapiFiiListResponse.FiiListItem item = response.fiis().get(0);
        assertThat(item.symbol()).isEqualTo("MXRF11");
        assertThat(item.price()).isEqualTo(10.52);
        assertThat(item.navPerShare()).isEqualTo(10.80);
        assertThat(item.priceToNav()).isEqualTo(0.974);
        assertThat(item.dividendYield12m()).isEqualTo(12.5);
        assertThat(item.totalInvestors().longValue()).isEqualTo(850_000L);
        assertThat(item.segmentType()).isEqualTo("papel");
        assertThat(item.administratorName()).isEqualTo("BTG Pactual");
        assertThat(item.administratorCnpj()).isEqualTo("30.822.936/0001-88");
    }

    @Test
    void fiiList_handlesNullFields() throws Exception {
        String json = """
                {
                  "fiis": [
                    { "symbol": "XPML11", "price": null, "navPerShare": null }
                  ]
                }
                """;

        BrapiFiiListResponse response = mapper.readValue(json, BrapiFiiListResponse.class);
        BrapiFiiListResponse.FiiListItem item = response.fiis().get(0);

        assertThat(item.symbol()).isEqualTo("XPML11");
        assertThat(item.price()).isNull();
        assertThat(item.navPerShare()).isNull();
    }

    @Test
    void fiiList_handlesEmptyList() throws Exception {
        String json = """
                { "fiis": [] }
                """;

        BrapiFiiListResponse response = mapper.readValue(json, BrapiFiiListResponse.class);
        assertThat(response.fiis()).isEmpty();
    }

    // ── FII Dividends ─────────────────────────────────────────────────────────

    @Test
    void fiiDividends_parsesFlatDividendsArray() throws Exception {
        String json = """
                {
                  "dividends": [
                    {
                      "symbol": "MXRF11",
                      "approvedOn": "2024-12-10",
                      "label": "RENDIMENTO",
                      "lastDatePrior": "2024-12-13",
                      "paymentDate": "2024-12-20",
                      "rate": 0.10,
                      "relatedTo": "DEC/2024",
                      "isinCode": "BRMXRFCTF003",
                      "remarks": ""
                    }
                  ]
                }
                """;

        BrapiFiiDividendsResponse response = mapper.readValue(json, BrapiFiiDividendsResponse.class);

        assertThat(response.dividends()).hasSize(1);
        BrapiFiiDividendsResponse.FiiDividend div = response.dividends().get(0);
        assertThat(div.symbol()).isEqualTo("MXRF11");
        assertThat(div.label()).isEqualTo("RENDIMENTO");
        assertThat(div.rate()).isEqualTo(0.10);
        assertThat(div.paymentDate()).isEqualTo("2024-12-20");
        assertThat(div.lastDatePrior()).isEqualTo("2024-12-13");
        assertThat(div.approvedOn()).isEqualTo("2024-12-10");
        assertThat(div.relatedTo()).isEqualTo("DEC/2024");
    }

    @Test
    void fiiDividends_ignoresUnknownFields() throws Exception {
        // brapi sometimes includes extra fields like isinCode
        String json = """
                {
                  "dividends": [
                    {
                      "symbol": "HGLG11",
                      "rate": 0.85,
                      "paymentDate": "2025-01-15",
                      "lastDatePrior": "2025-01-10",
                      "isinCode": "BRHGLGCTF009",
                      "extraField": "should not fail"
                    }
                  ]
                }
                """;

        BrapiFiiDividendsResponse response = mapper.readValue(json, BrapiFiiDividendsResponse.class);
        assertThat(response.dividends()).hasSize(1);
        assertThat(response.dividends().get(0).rate()).isEqualTo(0.85);
    }

    // ── FII Historical (price history) ────────────────────────────────────────

    @Test
    void fiiHistorical_parsesPriceBars() throws Exception {
        String json = """
                {
                  "fiis": [
                    {
                      "symbol": "MXRF11",
                      "historicalDataPrice": [
                        {
                          "date": 1704067200000,
                          "open": 10.40,
                          "high": 10.60,
                          "low": 10.35,
                          "close": 10.52,
                          "volume": 3500000,
                          "adjustedClose": 10.52
                        }
                      ]
                    }
                  ]
                }
                """;

        BrapiFiiHistoricalResponse response = mapper.readValue(json, BrapiFiiHistoricalResponse.class);

        assertThat(response.fiis()).hasSize(1);
        assertThat(response.fiis().get(0).symbol()).isEqualTo("MXRF11");
        assertThat(response.fiis().get(0).historicalDataPrice()).hasSize(1);

        BrapiFiiHistoricalResponse.PriceBar bar = response.fiis().get(0).historicalDataPrice().get(0);
        assertThat(bar.close()).isEqualTo(10.52);
        assertThat(bar.volume()).isEqualTo(3_500_000L);
        assertThat(bar.adjustedClose()).isEqualTo(10.52);
    }

    // ── Treasury List ─────────────────────────────────────────────────────────

    @Test
    void treasuryList_parsesRealBrapiResponse() throws Exception {
        // Reflects actual brapi /api/v2/treasury/list structure: root key is "results", fields are bondType not name/type
        String json = """
                {
                  "results": [
                    {
                      "symbol": "tesouro-ipca-com-juros-semestrais-15082026",
                      "bondType": "Tesouro IPCA+ com Juros Semestrais",
                      "indexer": "ipca",
                      "couponType": "semestral",
                      "maturityDate": "2026-08-15",
                      "durationDays": 44,
                      "buyRate": 11.4,
                      "sellRate": 11.52,
                      "buyPrice": 4816.98,
                      "sellPrice": 4813.73,
                      "basePrice": 4813.73,
                      "rateInfo": { "rateType": "realAnnualRateOverIpca" }
                    }
                  ]
                }
                """;

        BrapiTreasuryListResponse response = mapper.readValue(json, BrapiTreasuryListResponse.class);

        assertThat(response.results()).hasSize(1);
        BrapiTreasuryListResponse.TreasuryItem item = response.results().get(0);
        assertThat(item.symbol()).isEqualTo("tesouro-ipca-com-juros-semestrais-15082026");
        assertThat(item.bondType()).isEqualTo("Tesouro IPCA+ com Juros Semestrais");
        assertThat(item.indexer()).isEqualTo("ipca");
        assertThat(item.couponType()).isEqualTo("semestral");
        assertThat(item.maturityDate()).isEqualTo("2026-08-15");
        assertThat(item.durationDays()).isEqualTo(44);
        assertThat(item.buyRate()).isEqualTo(11.4);
        assertThat(item.buyPrice()).isEqualTo(4816.98);
    }

    @Test
    void treasuryHistory_parsesHistoricalRates() throws Exception {
        // History endpoint also uses "results" as root key
        String json = """
                {
                  "results": [
                    {
                      "symbol": "tesouro-selic-01032031",
                      "referenceDate": "2025-06-01",
                      "buyRate": 12.20,
                      "sellRate": 12.15,
                      "buyPrice": 14100.00,
                      "sellPrice": 14095.00,
                      "basePrice": 14080.00
                    }
                  ]
                }
                """;

        BrapiTreasuryHistoryResponse response = mapper.readValue(json, BrapiTreasuryHistoryResponse.class);

        assertThat(response.results()).hasSize(1);
        BrapiTreasuryHistoryResponse.TreasuryHistoryEntry entry = response.results().get(0);
        assertThat(entry.referenceDate()).isEqualTo("2025-06-01");
        assertThat(entry.buyRate()).isEqualTo(12.20);
        assertThat(entry.buyPrice()).isEqualTo(14100.00);
    }

    // ── Fund List ─────────────────────────────────────────────────────────────

    @Test
    void fundList_parsesAllFundTypes() throws Exception {
        // brapi /api/v2/funds/list uses "results" as root key (same pattern as treasury/list)
        String json = """
                {
                  "results": [
                    {
                      "symbol": "CPFF11",
                      "name": "CPF Fundo de Fundos FIAGRO",
                      "type": "FIAGRO",
                      "price": 9.80,
                      "dividendYield12m": 14.2,
                      "dividendYield1m": 1.1,
                      "priceToNav": 0.95,
                      "navPerShare": 10.32,
                      "totalInvestors": 50000,
                      "administratorName": "Itaú Unibanco",
                      "administratorCnpj": "60.701.190/0001-04",
                      "segmentType": "agro"
                    }
                  ]
                }
                """;

        BrapiFundListResponse response = mapper.readValue(json, BrapiFundListResponse.class);

        assertThat(response.results()).hasSize(1);
        BrapiFundListResponse.FundItem item = response.results().get(0);
        assertThat(item.symbol()).isEqualTo("CPFF11");
        assertThat(item.type()).isEqualTo("FIAGRO");
        assertThat(item.price()).isEqualTo(9.80);
        assertThat(item.dividendYield12m()).isEqualTo(14.2);
        assertThat(item.priceToNav()).isEqualTo(0.95);
        assertThat(item.totalInvestors().longValue()).isEqualTo(50_000L);
        assertThat(item.administratorName()).isEqualTo("Itaú Unibanco");
    }

    @Test
    void fundDividends_parsesPayouts() throws Exception {
        String json = """
                {
                  "dividends": [
                    {
                      "symbol": "CPFF11",
                      "approvedOn": "2025-05-15",
                      "label": "RENDIMENTO",
                      "lastDatePrior": "2025-05-20",
                      "paymentDate": "2025-05-30",
                      "rate": 0.12,
                      "relatedTo": "MAI/2025",
                      "remarks": ""
                    }
                  ]
                }
                """;

        BrapiFundDividendsResponse response = mapper.readValue(json, BrapiFundDividendsResponse.class);

        assertThat(response.dividends()).hasSize(1);
        BrapiFundDividendsResponse.FundDividend div = response.dividends().get(0);
        assertThat(div.symbol()).isEqualTo("CPFF11");
        assertThat(div.rate()).isEqualTo(0.12);
        assertThat(div.paymentDate()).isEqualTo("2025-05-30");
        assertThat(div.relatedTo()).isEqualTo("MAI/2025");
    }

    // ── Multi-item batch parsing ───────────────────────────────────────────────

    @Test
    void fiiList_parsesBatchOfMultipleFiis() throws Exception {
        String json = """
                {
                  "fiis": [
                    { "symbol": "MXRF11", "price": 10.52, "dividendYield12m": 12.5 },
                    { "symbol": "HGLG11", "price": 158.20, "dividendYield12m": 8.3 },
                    { "symbol": "XPML11", "price": 112.50, "dividendYield12m": 9.1 }
                  ]
                }
                """;

        BrapiFiiListResponse response = mapper.readValue(json, BrapiFiiListResponse.class);

        List<String> symbols = response.fiis().stream().map(BrapiFiiListResponse.FiiListItem::symbol).toList();
        assertThat(symbols).containsExactly("MXRF11", "HGLG11", "XPML11");
        assertThat(response.fiis().stream()
                .allMatch(f -> f.price() != null && f.dividendYield12m() != null)).isTrue();
    }
}
