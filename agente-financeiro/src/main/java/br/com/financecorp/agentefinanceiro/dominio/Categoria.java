package br.com.financecorp.agentefinanceiro.dominio;

/** Lista fechada de categorias de lançamento; valor fora dela é {@code CategoriaInvalidaException}. */
public enum Categoria {
    VENDAS,
    SERVICOS,
    PESSOAL,
    INFRAESTRUTURA,
    MARKETING,
    IMPOSTOS,
    VIAGENS,
    OUTRAS
}
