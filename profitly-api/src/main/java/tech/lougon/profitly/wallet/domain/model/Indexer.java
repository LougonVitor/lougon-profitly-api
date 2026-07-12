package tech.lougon.profitly.wallet.domain.model;

/** How a renda-fixa position's rate accrues: CDI/SELIC are pós-fixado, PREFIXADO is pré-fixado, IPCA is híbrido. */
public enum Indexer {
    CDI,
    SELIC,
    IPCA,
    PREFIXADO
}
