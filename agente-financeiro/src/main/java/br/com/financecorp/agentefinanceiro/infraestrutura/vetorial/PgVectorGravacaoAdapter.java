package br.com.financecorp.agentefinanceiro.infraestrutura.vetorial;

import static br.com.financecorp.agentefinanceiro.infraestrutura.vetorial.MetadadosTrecho.*;

import br.com.financecorp.agentefinanceiro.aplicacao.modelo.SituacaoDocumento;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.TrechoParaGravar;
import br.com.financecorp.agentefinanceiro.aplicacao.porta.GravacaoTrechosPort;
import br.com.financecorp.agentefinanceiro.infraestrutura.config.PropriedadesRag;
import br.com.financecorp.agentefinanceiro.infraestrutura.ia.ExecutorChamadaModelo;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Grava trechos no PgVectorStore em lotes; o embedding é gerado no {@code add}, fora de transação.
 * Remoção e troca de situação filtram por documentoId (constituição VI: apagar documento apaga
 * trechos e vetores).
 */
@Component
public final class PgVectorGravacaoAdapter implements GravacaoTrechosPort {

    private final VectorStore vetores;
    private final JdbcClient jdbc;
    private final ExecutorChamadaModelo executor;
    private final int lote;
    private final String modeloEmbedding;

    public PgVectorGravacaoAdapter(VectorStore vetores, JdbcClient jdbc, ExecutorChamadaModelo executor,
            PropriedadesRag propriedades, @Value("${spring.ai.openai.embedding.model}") String modeloEmbedding) {
        this.vetores = vetores;
        this.jdbc = jdbc;
        this.executor = executor;
        this.lote = propriedades.loteGravacao();
        this.modeloEmbedding = modeloEmbedding;
    }

    @Override
    public void gravar(List<TrechoParaGravar> trechos) {
        for (int inicio = 0; inicio < trechos.size(); inicio += lote) {
            List<Document> documentos = trechos.subList(inicio, Math.min(inicio + lote, trechos.size()))
                    .stream().map(this::documento).toList();
            executor.executar(() -> {
                vetores.add(documentos);
                return null;
            });
        }
    }

    @Override
    public void removerPorDocumento(UUID documentoId) {
        vetores.delete(new FilterExpressionBuilder().eq(DOCUMENTO_ID, documentoId.toString()).build());
    }

    /** A coluna metadata é json (formato do PgVectorStore); jsonb_set exige o cast de ida e volta. */
    @Override
    public void atualizarSituacao(UUID documentoId, SituacaoDocumento situacao) {
        jdbc.sql("""
                UPDATE conhecimento.vector_store
                   SET metadata = jsonb_set(metadata::jsonb, '{situacao}', to_jsonb(CAST(:situacao AS text)))::json
                 WHERE metadata ->> 'documentoId' = :documentoId
                """)
                .param("situacao", situacao.name())
                .param("documentoId", documentoId.toString())
                .update();
    }

    @Override
    public String modeloEmbedding() {
        return modeloEmbedding;
    }

    private Document documento(TrechoParaGravar t) {
        Map<String, Object> metadados = new HashMap<>();
        metadados.put(DOCUMENTO_ID, t.documentoId().toString());
        metadados.put(TITULO, t.titulo());
        metadados.put(VERSAO, t.versao());
        metadados.put(SECAO, t.secao());
        metadados.put(ORDEM, t.ordem());
        metadados.put(SITUACAO, t.situacao().name());
        metadados.put(MODELO_EMBEDDING, modeloEmbedding);
        metadados.put(ARQUIVO_ORIGEM, t.arquivoOrigem());
        metadados.put(CADASTRADO_POR, t.cadastradoPor());
        metadados.put(CADASTRADO_EM, t.cadastradoEm().toString());
        return Document.builder().id(t.id().toString()).text(t.texto()).metadata(metadados).build();
    }
}
