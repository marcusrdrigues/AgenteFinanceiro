package br.com.financecorp.agentefinanceiro.aplicacao.modelo;

/** Ciclo de vida do documento na base (data-model §1.3). Só {@link #AUTORIZADO} aparece na busca. */
public enum SituacaoDocumento {
    PROCESSANDO,
    AUTORIZADO,
    NAO_AUTORIZADO,
    EM_REVISAO,
    ERRO_PROCESSAMENTO
}
