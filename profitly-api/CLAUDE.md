# Profitly API — guia para o Claude

SaaS de acompanhamento de carteira de investimentos B3. Backend Java 21 / Spring Boot 4.1 (DDD + Clean Architecture), frontend React em `../../lougon-profitly-web/profitly-web` (repo separado — commits lá também).

## Regra inegociável

**NUNCA chamar o brapi durante requisição de usuário.** Todo dado é servido do banco. O brapi só é acessado por schedulers (crons noturnos 19h–20h BRT). Startup syncs existem mas ficam desligados por padrão (`profitly.sync.on-startup=false`) para economizar requisições do plano PRO.

## Arquitetura de dados

- Fonte: brapi.dev (token PRO via env `BRAPI_TOKEN`, header `Authorization` sem "Bearer")
- Postgres em Docker (container `postgres`, db `profitly`, user `postgres`) — consultas via `docker exec postgres psql -U postgres -d profitly -c "..."`
- `ddl-auto=update`: nunca mudar TIPO de coluna existente (criar coluna nova); respeitar limites como `tickers.sub_type varchar(30)`
- Padrão "raw JSON": demonstrativos e históricos de indicadores guardam a linha inteira do brapi em `stock_statements.raw_json` — nada se perde quando o brapi adiciona campos

## Fórmulas validadas (não regredir)

- **DY histórico** (bate com Investidor10, tolerância <0,9 p.p.): dividendos ajustados por split (÷ fator acumulado dos splits POSTERIORES à data-ex, tabela `stock_split_events`) ÷ fechamento do último pregão do ano. Preços do brapi são split-ajustados retroativamente; dividendos são valores da época.
- **Dividendos de units** (SANB11, KLBN11...): cadeia de 3 fallbacks no `BrapiAnalysisClient.fetchDividendsBatch` — v2 400 → divide lote → `/fii/dividends` → legado `/api/quote/{s}?dividends=true`. Passe de reparo re-tenta units zeradas ao fim do sync de ações.
- **P/L**: fallback preço ÷ LPA quando o brapi não manda `trailingPE` (aplicar também em médias setoriais; excluir negativos da média).

## Armadilhas conhecidas

- Spring Boot 4 usa **Jackson 3** nos codecs do WebClient: DTOs de client NÃO podem usar `JsonNode` do Jackson 2 — usar `Map<String, Object>`
- Ternário aninhado misturando `double` primitivo e `Double` anulável = NPE de auto-unboxing — usar if/else
- `@Transactional` em método chamado da própria classe não funciona (proxy) — usar `deleteAll(findBy...())` em vez de `@Modifying deleteBy...`
- PowerShell `Get-Content`+`Set-Content` corrompe UTF-8 dos fontes — usar as ferramentas Edit/Write
- Bancos não reportam EBIT/netIncome/marketCap: usar fallbacks (`cleanEbit`, `netIncomeFromContinuingOps`, `netIncomeApplicableToCommonShares`) e aceitar nulos
- brapi: `/api/v2/fii/list` e `/treasury/list` aceitam `limit=10000` (uma chamada traz tudo); `/api/v2/tickers` pagina com `subType=stock|unit|bdr|fidc|fip`; `/funds/list?assetType=` só tem fiagro/fiinfra
- brapi crypto (`/api/v2/crypto`): `marketCap` vem SEMPRE 0 (ranquear por volume); aceita `range`/`interval` mas `range=max` limita a ~1000 barras diárias (~2,7 anos); volume do histórico vem em unidades da moeda (fracionário), o da cotação em BRL; `currencyRateFromUSD` dá o câmbio usado

## Convenções de trabalho

- **Commit após cada alteração de arquivo/componente** (preferência do Vítor), mensagens conventional commits em inglês
- Compilar com `.\mvnw.cmd compile` (backend) e `npx tsc --noEmit` (frontend) antes de commitar
- Testes de parse de DTO em `BrapiDtoParseTest` — adicionar um caso ao criar DTO novo
- UI em pt-BR; seções somem quando não há dados (nunca mostrar card vazio)

## Cripto

- Histórico diário das moedas vai para `price_points` (mesma tabela das ações): BRL sob o símbolo da moeda, USD sob `{coin}:USD` — o gráfico genérico `/api/analysis/{symbol}/history` serve ambos sem código novo (toggle BRL/USD no frontend). A série BRL do brapi é sintética (USD × câmbio único do momento da chamada); a USD é a autêntica
- `CryptoSyncScheduler` (19h50 BRT): catálogo → cotações (lotes de 20) → histórico BRL e USD (backfill `range=max` em lotes de 5 para moedas sem histórico; incremental `3mo` em lotes de 20, inserindo só barras novas) → Fear & Greed
- `/api/crypto/analysis/{coin}` (`CryptoAnalysisService`): retornos por período, volatilidade anualizada (√365, cripto negocia todo dia), max drawdown 1a, ATH, faixa 52s, SMA50/200, ranking por volume — tudo calculado do banco
- **Fear & Greed** (`crypto_fear_greed`, exibido só na página do BTC): api.alternative.me/fng, sem chave, backfill `limit=0` (histórico completo, >256KB — client precisa de buffer maior), incremental `limit=30`; valores numéricos chegam como string
- Gráfico de cripto NÃO tem "vs IBOV" (prop `showBenchmark={false}` no `PriceChartSection`)
- `profitly.sync.crypto-on-startup=false` — ligar só durante dev ativo da tela de cripto (cron das 19h50 mantém os dados)

## Tesouro Direto

- Símbolos são minúsculos (`tesouro-prefixado-01012029`) — o `/api/analysis/{symbol}` genérico tenta o símbolo como veio e só depois uppercase (não voltar a fazer uppercase incondicional)
- `TreasurySyncScheduler` (19h45 BRT): `/api/v2/treasury/list` (1 chamada traz os ~60 títulos) → salva `treasury_bonds` + upsert em `tickers` → histórico em `treasury_bond_history` via `/treasury/indicators/history` em lotes de 20 símbolos (backfill desde 2020 quando o título não tem linhas — brapi só tem dados desde ~2022-02; incremental 3 meses nos demais)
- Resposta do history é ANINHADA: `results[].history[]` com `baseDate` (não é lista plana); `/treasury/indicators` usa root `results` (não `treasuries`), mesmo shape do list
- **rateInfo importa**: para Tesouro Selic `buyRate`/`sellRate` são SPREAD sobre a Selic (ex.: 0,08), não a rentabilidade total — exibir "SELIC + x%"; prefixado é taxa nominal, IPCA+ é taxa real. `rate_type`/`rate_unit`/`rate_description` ficam em `treasury_bonds`
- `/api/treasury/analysis/{symbol}` (`TreasuryAnalysisService`): variação da taxa por período (p.p.), faixa 52s da taxa, extremos históricos, retornos de marcação a mercado (sellPrice), volatilidade anualizada √252 (pregões), drawdown 1a, ranking por taxa dentro do indexador, títulos irmãos — tudo do banco
- Tela segue o layout da de cripto: barra de 5 métricas, gráfico com toggle de métrica + ranges 3M–Máx, grids de variação/retornos, cards de risco/extremos, tabela clicável de títulos do mesmo indexador
- **`profitly.sync.treasury-on-startup=true` é TEMPORÁRIO** (dev da tela de tesouro) — voltar para false ao concluir

## Estado (2026-07-05)

- **Completo:** tela de ações (benchmark IBOV, 30 indicadores c/ histórico, comparação setorial, preço justo Graham/Bazin/Gordon, agenda de proventos, demonstrativos 12M/Atual), comparador `/comparar`, syncs de todos os tipos de ativo, tela de criptoativos (retornos, risco, ATH, médias móveis, gráfico BRL/USD, Fear & Greed no BTC), tela de Tesouro Direto (histórico de taxas, faixa 52s, marcação a mercado, comparação por indexador)
- **Frente atual:** refinamento da tela de Tesouro Direto
- **Backlog:** tela FII no padrão da de ações; carteira integrando tesouro/cripto/fundos; i18n das seções novas; responsividade mobile; desativar `profitly.sync.treasury-on-startup` ao fim do dev de tesouro
