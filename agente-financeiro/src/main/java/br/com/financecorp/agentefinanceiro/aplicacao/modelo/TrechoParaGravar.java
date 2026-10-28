package br.com.financecorp.agentefinanceiro.aplicacao.modelo;

import java.time.Instant;
import java.util.UUID;

/** Trecho a gravar na base vetorial, com todos os metadados obrigatórios (constituição VI). */
public record TrechoParaGravar(
        UUID id,
        UUID documentoId,
        int ordem,
        String titulo,
        String versao,
        String secao,
        String texto,
        SituacaoDocumento situacao,
        String arquivoOrigem,
        String cadastradoPor,
        Instant cadastradoEm) {
}
