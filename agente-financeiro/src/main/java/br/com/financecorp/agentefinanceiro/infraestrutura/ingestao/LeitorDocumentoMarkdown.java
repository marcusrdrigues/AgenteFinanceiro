package br.com.financecorp.agentefinanceiro.infraestrutura.ingestao;

import br.com.financecorp.agentefinanceiro.aplicacao.modelo.DocumentoParaIngestao;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.SituacaoDocumento;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * Lê um documento de política em Markdown com front matter simples ({@code chave: valor} entre
 * linhas {@code ---}). Sem biblioteca de YAML: o formato é fechado e a dependência não se paga.
 */
final class LeitorDocumentoMarkdown {

    /** Front matter ausente ou incompleto: o arquivo é recusado inteiro. */
    static final class DocumentoInvalidoException extends RuntimeException {
        DocumentoInvalidoException(String arquivo, String motivo) {
            super("Documento " + arquivo + " inválido: " + motivo);
        }
    }

    private LeitorDocumentoMarkdown() {
    }

    static DocumentoParaIngestao ler(String arquivo, String conteudo) {
        String texto = conteudo.replace("\r\n", "\n");
        if (!texto.startsWith("---\n")) {
            throw new DocumentoInvalidoException(arquivo, "falta o front matter");
        }
        int fim = texto.indexOf("\n---\n", 4);
        if (fim < 0) {
            throw new DocumentoInvalidoException(arquivo, "front matter sem fechamento");
        }
        Map<String, String> campos = new HashMap<>();
        for (String linha : texto.substring(4, fim).split("\n")) {
            int separador = linha.indexOf(':');
            if (separador > 0) {
                campos.put(linha.substring(0, separador).strip(), linha.substring(separador + 1).strip());
            }
        }
        String corpo = texto.substring(fim + 5);
        return new DocumentoParaIngestao(arquivo, obrigatorio(campos, "titulo", arquivo),
                obrigatorio(campos, "versao", arquivo), data(campos, arquivo), situacao(campos, arquivo),
                "aprovada".equals(campos.get("revisaoHumana")), corpo);
    }

    private static String obrigatorio(Map<String, String> campos, String chave, String arquivo) {
        String valor = campos.get(chave);
        if (valor == null || valor.isBlank()) {
            throw new DocumentoInvalidoException(arquivo, "falta o campo " + chave);
        }
        return valor;
    }

    private static LocalDate data(Map<String, String> campos, String arquivo) {
        try {
            return LocalDate.parse(obrigatorio(campos, "vigenteDesde", arquivo));
        } catch (DateTimeParseException e) {
            throw new DocumentoInvalidoException(arquivo, "vigenteDesde fora do formato AAAA-MM-DD");
        }
    }

    /** O conjunto curado só declara AUTORIZADO ou NAO_AUTORIZADO; as demais situações são da esteira. */
    private static SituacaoDocumento situacao(Map<String, String> campos, String arquivo) {
        String valor = obrigatorio(campos, "situacao", arquivo);
        return switch (valor) {
            case "AUTORIZADO" -> SituacaoDocumento.AUTORIZADO;
            case "NAO_AUTORIZADO" -> SituacaoDocumento.NAO_AUTORIZADO;
            default -> throw new DocumentoInvalidoException(arquivo, "situacao deve ser AUTORIZADO ou NAO_AUTORIZADO");
        };
    }
}
