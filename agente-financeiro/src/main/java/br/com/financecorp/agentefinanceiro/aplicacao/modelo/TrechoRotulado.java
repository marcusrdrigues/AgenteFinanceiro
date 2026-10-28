package br.com.financecorp.agentefinanceiro.aplicacao.modelo;

/** Trecho com o rótulo que o modelo enxerga (T1, T2...). O id interno do documento nunca vai ao prompt. */
public record TrechoRotulado(String rotulo, TrechoDocumento trecho) {
}
