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
        // brapi /api/v2/funds/list uses "funds" as root key
        String json = """
                {
                  "funds": [
                    {
                      "symbol": "JURO11",
                      "cnpj": "42730834000100",
                      "name": "SPARTA INFRA",
                      "legalName": "SPARTA INFRA FIC FI INFRA RENDA FIXA CP",
                      "assetType": "fiinfra",
                      "b3Classification": "Financeiro/Fundos/FI-INFRA",
                      "price": 96.99,
                      "navPerShare": 99.19945,
                      "priceToNav": 0.9777272,
                      "equity": 2040699000,
                      "totalAssets": 2041704100,
                      "totalInvestors": 92710
                    }
                  ],
                  "pagination": { "page": 1, "totalItems": 1, "totalPages": 1, "hasNextPage": false }
                }
                """;

        BrapiFundListResponse response = mapper.readValue(json, BrapiFundListResponse.class);

        assertThat(response.funds()).hasSize(1);
        BrapiFundListResponse.FundItem item = response.funds().get(0);
        assertThat(item.symbol()).isEqualTo("JURO11");
        assertThat(item.assetType()).isEqualTo("fiinfra");
        assertThat(item.price()).isEqualTo(96.99);
        assertThat(item.priceToNav()).isEqualTo(0.9777272);
        assertThat(item.equity()).isEqualTo(2040699000.0);
        assertThat(item.totalInvestors().longValue()).isEqualTo(92_710L);
        assertThat(item.legalName()).isEqualTo("SPARTA INFRA FIC FI INFRA RENDA FIXA CP");
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

    // ── Stock endpoints ────────────────────────────────────────────────────────

    @Test
    void stockQuote_parsesQuoteData() throws Exception {
        String json = """
                {
                  "results": [
                    {
                      "requestedSymbol": "PETR4",
                      "symbol": "PETR4",
                      "data": {
                        "shortName": "PETROBRAS   PN  EX  N2",
                        "longName": "Petróleo Brasileiro S.A. - Petrobras",
                        "currency": "BRL",
                        "regularMarketPrice": 41.18,
                        "regularMarketDayHigh": 41.53,
                        "regularMarketDayLow": 40.82,
                        "regularMarketChange": -0.58,
                        "regularMarketChangePercent": -1.39,
                        "regularMarketTime": "2026-06-14T05:15:42.000Z",
                        "marketCap": null,
                        "regularMarketVolume": 34024700,
                        "regularMarketPreviousClose": 41.76,
                        "regularMarketOpen": 41.18,
                        "fiftyTwoWeekLow": 29.31,
                        "fiftyTwoWeekHigh": 50.69,
                        "logourl": "https://icons.brapi.dev/icons/PETR4.svg"
                      }
                    }
                  ]
                }
                """;

        BrapiStockQuoteResponse response = mapper.readValue(json, BrapiStockQuoteResponse.class);

        assertThat(response.results()).hasSize(1);
        var data = response.results().get(0).data();
        assertThat(response.results().get(0).symbol()).isEqualTo("PETR4");
        assertThat(data.regularMarketPrice()).isEqualTo(41.18);
        assertThat(data.fiftyTwoWeekHigh()).isEqualTo(50.69);
        assertThat(data.logoUrl()).contains("PETR4.svg");
        assertThat(data.regularMarketTime()).isEqualTo("2026-06-14T05:15:42.000Z");
    }

    @Test
    void stockProfile_parsesSectorAndCompanyInfo() throws Exception {
        String json = """
                {
                  "results": [
                    {
                      "requestedSymbol": "VVAR3",
                      "symbol": "BHIA3",
                      "changed": true,
                      "data": {
                        "city": "SÃO PAULO",
                        "state": "SP",
                        "website": "https://ri.grupocasasbahia.com.br",
                        "industry": "Eletrodomésticos",
                        "industryKey": "eletrodomesticos",
                        "sector": "Consumo Cíclico",
                        "sectorKey": "consumo-ciclico",
                        "longBusinessSummary": "O Grupo Casas Bahia S.A...",
                        "fullTimeEmployees": 57500,
                        "cnpj": "33041260065290",
                        "logoUrl": "https://icons.brapi.dev/icons/BHIA3.svg"
                      }
                    }
                  ]
                }
                """;

        BrapiStockProfileResponse response = mapper.readValue(json, BrapiStockProfileResponse.class);

        assertThat(response.results()).hasSize(1);
        var r = response.results().get(0);
        assertThat(r.symbol()).isEqualTo("BHIA3");
        assertThat(r.data().sector()).isEqualTo("Consumo Cíclico");
        assertThat(r.data().industry()).isEqualTo("Eletrodomésticos");
        assertThat(r.data().fullTimeEmployees()).isEqualTo(57500L);
        assertThat(r.data().cnpj()).isEqualTo("33041260065290");
    }

    @Test
    void stockStatements_preservesAllFieldsAsRawJson() throws Exception {
        String json = """
                {
                  "results": [
                    {
                      "requestedSymbol": "PETR4",
                      "symbol": "PETR4",
                      "data": [
                        {
                          "type": "yearly",
                          "endDate": "2025-12-31",
                          "totalRevenue": 497549000000,
                          "netIncome": 110605000000,
                          "someFutureField": "must survive"
                        }
                      ]
                    }
                  ]
                }
                """;

        BrapiStockStatementsResponse response = mapper.readValue(json, BrapiStockStatementsResponse.class);

        assertThat(response.results()).hasSize(1);
        var row = response.results().get(0).data().get(0);
        assertThat(row.get("endDate").asText()).isEqualTo("2025-12-31");
        assertThat(row.get("type").asText()).isEqualTo("yearly");
        assertThat(row.get("netIncome").asLong()).isEqualTo(110605000000L);
        // raw JSON keeps unknown fields — nothing is lost when brapi adds columns
        assertThat(row.get("someFutureField").asText()).isEqualTo("must survive");
    }
}
