package br.com.financecorp.agentefinanceiro.infraestrutura.vetorial;

/** Chaves do metadata de cada trecho, num único lugar (rag-pgvector §1). */
final class MetadadosTrecho {

    static final String DOCUMENTO_ID = "documentoId";
    static final String TITULO = "titulo";
    static final String VERSAO = "versao";
    static final String SECAO = "secao";
    static final String ORDEM = "ordem";
    static final String SITUACAO = "situacao";
    static final String MODELO_EMBEDDING = "modeloEmbedding";
    static final String ARQUIVO_ORIGEM = "arquivoOrigem";
    static final String CADASTRADO_POR = "cadastradoPor";
    static final String CADASTRADO_EM = "cadastradoEm";

    private MetadadosTrecho() {
    }
}
