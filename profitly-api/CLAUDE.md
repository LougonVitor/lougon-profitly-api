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
- **Renda+/Educa+ NÃO usam o ano do vencimento no nome oficial**: o `maturityDate` da brapi é a ÚLTIMA parcela; o nome usa o ano em que a renda começa (Renda+ = vencimento − 19, são 240 parcelas mensais; Educa+ = vencimento − 4, são 60). Ex.: vencimento 2084 = "Renda+ 2065". Cálculo em `incomeYearsBeforeMaturity` (scheduler) e `treasuryIncomeYears` (frontend)
- `/api/treasury/analysis/{symbol}` (`TreasuryAnalysisService`): variação da taxa por período (p.p.), faixa 52s da taxa, extremos históricos, retornos de marcação a mercado (sellPrice), volatilidade anualizada √252 (pregões), drawdown 1a, ranking por taxa dentro do indexador, títulos irmãos — tudo do banco
- Tela segue o layout da de cripto: barra de 5 métricas, gráfico com toggle de métrica + ranges 3M–Máx, grids de variação/retornos, cards de risco/extremos, tabela clicável de títulos do mesmo indexador
- `profitly.sync.treasury-on-startup=false` — ligar só durante dev ativo da tela de tesouro (cron das 19h45 mantém os dados)

## Fundos listados (FIAGRO / FI-Infra / FIDC / FIP)

- **Cobertura brapi é desigual**: `/api/v2/funds/list?assetType=` só tem fiagro (~31) e fiinfra (só JURO11!); fidc/fip retornam vazio mesmo por `symbols=`. Os listados na B3 (BDIV11, XPIE11...) são semeados da tabela `tickers` (`asset_type=fund`, `sub_type` em fiagro/fi-agro/fiinfra/fi-infra/fidc/fip — normalizar variantes de grafia!). NUNCA semear todos os `asset_type=fund`: inclui 175 ETFs + 46 FIIs
- `/funds/dividends?symbols=` funciona para TODOS os fundos listados (até fidc/fip fora do list) e traz `cnpj`/`assetType` — usado para enriquecer o catálogo. DY 12m/1m é calculado dos eventos ÷ preço (brapi manda null)
- **`/v2/funds/dividends` só tem os últimos 12 MESES** (mesmo com `startDate` antigo — `totalItems` limita). Histórico completo vem do legado `/api/quote/{s}?dividends=true` no primeiro backfill; MAS o legado usa a data de ANÚNCIO como `paymentDate` (~2 semanas antes do v2), então mesclar por data duplicaria a janela de overlap — só entram eventos anteriores ao MÊS do evento mais antigo do v2 (`backfillLegacyDividends`)
- `/funds/nav/history` só tem dados de fundos-FIF (JURO11 diário); fiagro/fip não têm — a tela degrada (seções somem). Resposta é lista PLANA (`history[]`, um item por símbolo+data) com paginação, diferente do tesouro
- Histórico de PREÇO de mercado vem de `/api/v2/stocks/historical` (funciona para qualquer ticker de fundo) → vai para `price_points` → gráfico genérico `/api/analysis/{symbol}/history` serve sem código novo
- Documentos regulatórios (profile, portfolio, fiagro/fidc reports+portfolio, fip reports) ficam como raw JSON em `fund_documents` (symbol + doc_type + reference_date); shapes variam por tipo — DTO genérico `BrapiFundRawListResponse` com `Map<String,Object>` e `@JsonAlias({"funds","profiles","reports"})`
- `FundSyncScheduler` (19h40 BRT): list → seed tickers → indicators (lotes 20) → NAV history (backfill 2020/incremental 3m) → dividendos (backfill total/incremental 3m) → DY calculado → documentos → preço de mercado (backfill max/incremental 3mo) → variação diária do ticker (últimos 2 fechamentos, fallback NAV)
- `/api/funds/analysis/{symbol}` (`FundAnalysisService`): retornos de preço E de NAV por período, vol √252 de ambos, drawdown, faixa 52s, extremos de NAV, evolução patrimônio/cotistas, ranking por DY dentro do tipo, fundos irmãos, dividendos recentes, documentos raw desserializados por tipo — tudo do banco
- Tela (`FundAnalysisPage` no `TickerAnalysis.tsx`): barra de 5 métricas, gráfico, faixa 52s, grids de retorno preço/NAV, cards risco/patrimônio, composição da carteira por tipo (fiagro=allocations, fidc=sectors+cedentes, genérico=summary, fip=capital), perfil de cotistas, tabela de rendimentos, comparação clicável por tipo. Datas dentro de raw docs vêm como ISO completo — `docDate()` corta em 10 chars
- `profitly.sync.funds-on-startup=false` — ligar só durante dev ativo da tela de fundos (cron das 19h40 mantém os dados)

## Fundos imobiliários (FII) — tela avançada, tabelas próprias

FII tem vertical dedicada e ISOLADA de Fundos (não reusar as tabelas/serviços de fundos): `fii_indicators`, `fii_indicator_history`, `fii_dividend_events`, `fii_documents`. Serviço `FiiAnalysisService`, scheduler `FiiIndicatorSyncScheduler` (19h30 BRT), controller `/api/fii/**`, tela `FiiAnalysisPage` no `TickerAnalysis.tsx` (dispatch por `isFii`, ANTES de `isFund`).

- **TODOS os endpoints `/api/v2/fii/*` aceitam até 20 símbolos por chamada** — batelar tudo em lotes de 20 (indicators, history, dividends, properties, portfolio, reports). NÃO chamar 1 por FII: são ~1028 FIIs, vira milhares de requisições e o sync trava antes das fases finais (foi o bug original: dividendos/documentos/preços nunca populavam)
- **`/fii/list` NÃO traz** equity/totalAssets/sharesOutstanding/dividendYield1m/monthlyReturn/asOfDate — só vêm de **`/fii/indicators`** (enriquecer o catálogo em lote). A resposta de `/fii/indicators` é **FLAT** (campos no topo, não aninhada em `data`/`administrator` — o DTO aninhado antigo nunca parseava porque o método não era usado)
- **DY vem como FRAÇÃO** (0,12 = 12%): multiplicar por 100 na exibição (`pct()` no serviço). `monthlyReturn` idem
- **`dividendYield1m` da brapi vem 0/null com frequência** (ex.: XPML11) — calcular do último rendimento ÷ preço atual (bate com "Yield 1 mês" do Investidor10). Idem yields 3m/6m = soma dos rendimentos do período ÷ preço
- **`/fii/historical` usa `startDate`/`endDate`, NÃO `range`** (passar `range` é silenciosamente ignorado → só volta 12 meses). Backfill com `startDate=2015-01-01`; batelar por 20 (buffer WebClient é 10MB, suporta ~20 FIIs com histórico completo). Vai para `price_points` → gráfico genérico `/api/analysis/{symbol}/history` (resposta sob a chave `prices`)
- **Vacância: filings ruins da brapi** — às vezes o trimestre mais recente marca TODOS os imóveis como ~100% vagos (ex.: XPML11 2026-Q1 = 91,8%, real ~4%). Ignorar vacância de resumo ≥60% (`IMPLAUSIBLE_VACANCY`): usar o trimestre sadio mais recente para o valor e a tendência, e zerar a coluna de vacância por imóvel (mantendo nome/área/receita). Só segmento `tijolo` tem imóveis; `papel` degrada (seções somem)
- **Magic Number** (destaque, 1º card): preço da cota ÷ último rendimento por cota = quantas cotas para 1 renda extra/mês
- **Taxa de administração**: `/fii/reports` traz `adminFeeRate` MENSAL (fração do PL). Guardar ~13 meses (doc_type `report`, vários por fundo) e usar a **MEDIANA dos últimos 12 × 12** (robusta a meses com taxa de performance). É a taxa EFETIVA paga — difere da estatutária do Investidor10 (HGLG 0,57%, VISC 1,08% batem; XPML 0,73% vs 0,55% estatutária)
- **DY médio 5 anos**: (média dos dividendos anuais dos últimos 5 anos completos) ÷ **preço ATUAL** — assim bate com o Investidor10 (XPML 8,77% vs 8,80%). NÃO usar preço de cada época (dá ~11%) nem média dos DY-12m mensais (dá ~11%)
- **Liquidez diária média**: média de `close × volume` dos últimos ~21 pregões (`price_points`)
- Datas de dividendos vêm com hora/tz ("2026-05-29 00:00:00+00") — cortar em 10 chars (`date10`) no serviço (o `fmtDateOnly` do front quebra ao dividir por "-")
- Composição da carteira: `/fii/portfolio` → `allocations[]` por `assetClass` (cri, fii, real_estate, real_estate_company...) → `AllocationBars`
- `FiiIndicatorSyncScheduler`: list → saveCurrent → syncIndicators (lote 20, enriquece) → indicator history (backfill 2016/incremental 3m) → dividendos (dedup por symbol+paymentDate+rate) → documentos (reports 13m + properties/portfolio + *_history) → preço (backfill 2015/incremental 3m) → variação diária
- `profitly.sync.fii-on-startup=false` — ligar só durante dev ativo da tela de FII (cron das 19h30 mantém os dados). **Sync completo dos 1028 FIIs leva ~10-15 min** e a brapi limita (rate limit) — erros intermitentes "Token não fornecido" são normais, o sync trata por lote

## Estado (2026-07-06)

- **Completo:** tela de ações (benchmark IBOV, 30 indicadores c/ histórico, comparação setorial, preço justo Graham/Bazin/Gordon, agenda de proventos, demonstrativos 12M/Atual), comparador `/comparar`, syncs de todos os tipos de ativo, tela de criptoativos (retornos, risco, ATH, médias móveis, gráfico BRL/USD, Fear & Greed no BTC), tela de Tesouro Direto (histórico de taxas, faixa 52s, marcação a mercado, comparação por indexador, nomes oficiais Renda+/Educa+), tela de fundos fiagro/fi-infra/fidc/fip (análise avançada, composição de carteira, histórico completo de rendimentos paginado, comparação por tipo paginada), **tela de FIIs no padrão avançado** (Magic Number, DY 1m/3m/6m/12m, DY médio 5a, liquidez diária, taxa de administração efetiva, vacância + imóveis, composição de carteira, faixa 52s, retornos preço/VP, comparação por segmento — todos batendo com o Investidor10)
- **Frente atual:** — (FII encerrado; `fii-on-startup` desligado. Só ~228/1028 FIIs têm cotação completa: o cron das 19h30 ou um sync manual completa a cauda longa)
- **Backlog:** carteira integrando tesouro/cripto/fundos/FIIs (usar `fund_dividend_events`/`fii_dividend_events` p/ proventos recebíveis); i18n das seções novas; responsividade mobile
