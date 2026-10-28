package br.com.financecorp.agentefinanceiro.aplicacao.modelo;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Documento lido do conjunto curado.
 *
 * @param situacaoDeclarada     {@code AUTORIZADO} ou {@code NAO_AUTORIZADO}, conforme o front matter
 * @param revisaoHumanaAprovada front matter {@code revisaoHumana: aprovada}: um revisor conferiu um
 *                              trecho que parece injeção e liberou a publicação; o trecho continua
 *                              tratado como dado no prompt
 */
public record DocumentoParaIngestao(
        String arquivoOrigem,
        String titulo,
        String versao,
        LocalDate vigenteDesde,
        SituacaoDocumento situacaoDeclarada,
        boolean revisaoHumanaAprovada,
        String texto) {

    public DocumentoParaIngestao {
        Objects.requireNonNull(arquivoOrigem, "arquivoOrigem");
        Objects.requireNonNull(titulo, "titulo");
        Objects.requireNonNull(versao, "versao");
        Objects.requireNonNull(vigenteDesde, "vigenteDesde");
        Objects.requireNonNull(situacaoDeclarada, "situacaoDeclarada");
        Objects.requireNonNull(texto, "texto");
    }
}
