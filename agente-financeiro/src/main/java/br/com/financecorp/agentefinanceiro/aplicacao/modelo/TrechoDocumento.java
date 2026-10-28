package br.com.financecorp.agentefinanceiro.aplicacao.modelo;

import java.util.UUID;

/**
 * Trecho recuperado da base de políticas. Não carrega vetor nem metadado interno (ordem, situação,
 * modelo de embedding).
 *
 * @param similaridade {@code 1 − distância do cosseno} (faixa −1..1); nula quando a busca não informa
 */
public record TrechoDocumento(
        UUID documentoId, String titulo, String versao, String secao, String texto, Double similaridade) {
}
