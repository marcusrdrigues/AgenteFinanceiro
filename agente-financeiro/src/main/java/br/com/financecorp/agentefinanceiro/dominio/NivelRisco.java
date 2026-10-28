package br.com.financecorp.agentefinanceiro.dominio;

/** {@link #INDETERMINADO} quando faltam receitas ou despesas para avaliar (US04 cenário 6). */
public enum NivelRisco {
    BAIXO,
    MODERADO,
    ALTO,
    INDETERMINADO
}
