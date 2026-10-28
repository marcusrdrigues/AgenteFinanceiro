package br.com.financecorp.agentefinanceiro.infraestrutura.vetorial;

import static br.com.financecorp.agentefinanceiro.infraestrutura.vetorial.MetadadosTrecho.*;

import br.com.financecorp.agentefinanceiro.aplicacao.modelo.SituacaoDocumento;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.TrechoDocumento;
import br.com.financecorp.agentefinanceiro.aplicacao.porta.BuscaDocumentoPort;
import br.com.financecorp.agentefinanceiro.dominio.excecao.FonteDadosIndisponivelException;
import br.com.financecorp.agentefinanceiro.infraestrutura.config.PropriedadesRag;
import br.com.financecorp.agentefinanceiro.infraestrutura.ia.ExecutorChamadaModelo;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

/**
 * Busca semântica com o filtro dentro da busca vetorial: só AUTORIZADO e só vetores do modelo de
 * embedding atual. Os valores do filtro vêm do código, nunca da pergunta.
 *
 * <p>Score conferido no fonte do PgVectorStore 2.0.2 com COSINE_DISTANCE: {@code score = 1 − (embedding
 * <=> consulta)} e o limiar entra no SQL como {@code distância < 1 − limiar}. A distância do cosseno vai
 * de 0 a 2, então o score vai de −1 a 1; o limiar 0.55 equivale a distância menor que 0.45.
 */
@Component
public final class PgVectorBuscaDocumentoAdapter implements BuscaDocumentoPort {

    private final VectorStore vetores;
    private final ExecutorChamadaModelo executor;
    private final PropriedadesRag propriedades;
    private final Filter.Expression filtro;

    public PgVectorBuscaDocumentoAdapter(VectorStore vetores, ExecutorChamadaModelo executor,
            PropriedadesRag propriedades, @Value("${spring.ai.openai.embedding.model}") String modeloEmbedding) {
        this.vetores = vetores;
        this.executor = executor;
        this.propriedades = propriedades;
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        this.filtro = b.and(b.eq(SITUACAO, SituacaoDocumento.AUTORIZADO.name()),
                b.eq(MODELO_EMBEDDING, modeloEmbedding)).build();
    }

    @Override
    public List<TrechoDocumento> buscar(String pergunta) {
        SearchRequest requisicao = SearchRequest.builder()
                .query(pergunta)
                .topK(propriedades.topK())
                .similarityThreshold(propriedades.limiarSimilaridade())
                .filterExpression(filtro)
                .build();
        try {
            // A consulta gera o embedding da pergunta: chamada ao provedor, por isso passa pelo executor.
            return executor.executar(() -> vetores.similaritySearch(requisicao)).stream()
                    .map(PgVectorBuscaDocumentoAdapter::trecho)
                    .toList();
        } catch (DataAccessException banco) {
            throw new FonteDadosIndisponivelException();
        }
    }

    /** Só o que pode sair: metadados internos (ordem, situação, modelo) e vetor ficam para trás. */
    private static TrechoDocumento trecho(Document d) {
        Map<String, Object> m = d.getMetadata();
        return new TrechoDocumento(UUID.fromString(String.valueOf(m.get(DOCUMENTO_ID))), texto(m, TITULO),
                texto(m, VERSAO), texto(m, SECAO), d.getText(), d.getScore());
    }

    private static String texto(Map<String, Object> m, String chave) {
        Object valor = m.get(chave);
        return valor == null ? "" : valor.toString();
    }
}
