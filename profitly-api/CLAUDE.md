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

## Convenções de trabalho

- **Commit após cada alteração de arquivo/componente** (preferência do Vítor), mensagens conventional commits em inglês
- Compilar com `.\mvnw.cmd compile` (backend) e `npx tsc --noEmit` (frontend) antes de commitar
- Testes de parse de DTO em `BrapiDtoParseTest` — adicionar um caso ao criar DTO novo
- UI em pt-BR; seções somem quando não há dados (nunca mostrar card vazio)

## Estado (2026-07-05)

- **Completo:** tela de ações (benchmark IBOV, 30 indicadores c/ histórico, comparação setorial, preço justo Graham/Bazin/Gordon, agenda de proventos, demonstrativos 12M/Atual), comparador `/comparar`, syncs de todos os tipos de ativo
- **Frente atual:** tela de criptoativos (dados já em `crypto_coins`/`crypto_quotes`, endpoints `/api/crypto/*`)
- **Backlog:** tela FII no padrão da de ações; carteira integrando tesouro/cripto/fundos; i18n das seções novas; responsividade mobile
