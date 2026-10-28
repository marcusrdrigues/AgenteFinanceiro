package br.com.financecorp.agentefinanceiro.aplicacao.modelo;

/**
 * Tokens do modelo de chat consumidos numa requisição. Nulos quando nenhuma chamada ao modelo foi
 * feita (por exemplo, pergunta recusada antes de classificar) ou quando o provedor não informou o uso.
 * Não inclui os tokens do modelo de embedding, que passam pelo VectorStore.
 */
public record Consumo(Integer tokensEntrada, Integer tokensSaida) {

    public static final Consumo NENHUM = new Consumo(null, null);
}
