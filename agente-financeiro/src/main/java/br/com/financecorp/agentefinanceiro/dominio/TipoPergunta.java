package br.com.financecorp.agentefinanceiro.dominio;

/** Lista fechada de classificação da pergunta (FR-039). Cada tipo segue um caminho fixo (FR-040). */
public enum TipoPergunta {
    NUMERICA,
    DOCUMENTAL,
    MISTA,
    ANALISE,
    RISCO_ESPECIALIZADO,
    FORA_DO_DOMINIO
}
