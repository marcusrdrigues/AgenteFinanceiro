package br.com.financecorp.agentefinanceiro.aplicacao.modelo;

import java.util.UUID;

public record DocumentoRegistrado(
        UUID id, String titulo, String versao, String arquivoOrigem, String hashConteudo, SituacaoDocumento situacao) {
}
