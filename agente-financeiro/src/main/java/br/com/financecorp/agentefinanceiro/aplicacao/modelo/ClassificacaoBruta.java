package br.com.financecorp.agentefinanceiro.aplicacao.modelo;

/**
 * Classificação como o modelo devolveu, antes da validação. Campo vazio significa "não informado".
 * Só {@code ValidadorClassificacao} transforma isto em {@link ClassificacaoPergunta}.
 */
public record ClassificacaoBruta(String tipo, String perguntaAutonoma, String inicio, String fim, String categoria) {
}
