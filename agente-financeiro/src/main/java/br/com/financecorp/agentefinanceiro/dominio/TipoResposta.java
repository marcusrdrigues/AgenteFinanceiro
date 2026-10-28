package br.com.financecorp.agentefinanceiro.dominio;

/**
 * Tipo devolvido na resposta. Repete a classificação e acrescenta {@link #NAO_INTERPRETADA}, que
 * só existe na resposta: o classificador nunca pode produzi-la (FR-041).
 */
public enum TipoResposta {
    NUMERICA,
    DOCUMENTAL,
    MISTA,
    ANALISE,
    RISCO_ESPECIALIZADO,
    FORA_DO_DOMINIO,
    NAO_INTERPRETADA;

    public static TipoResposta de(TipoPergunta tipo) {
        return valueOf(tipo.name());
    }
}
