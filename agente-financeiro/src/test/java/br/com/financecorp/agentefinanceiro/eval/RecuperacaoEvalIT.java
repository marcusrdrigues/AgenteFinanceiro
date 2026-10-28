package br.com.financecorp.agentefinanceiro.eval;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.financecorp.agentefinanceiro.aplicacao.modelo.TrechoDocumento;
import br.com.financecorp.agentefinanceiro.aplicacao.porta.BuscaDocumentoPort;
import br.com.financecorp.agentefinanceiro.infraestrutura.config.PropriedadesRag;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import tools.jackson.databind.JsonNode;

/**
 * Eval de recuperação, medido separado da resposta. Dois lados do limiar:
 * <ul>
 *   <li>positivos: o trecho certo veio entre os top-k? (SC-003 ≥ 90%)</li>
 *   <li>negativos ({@code "titulo": null}): pergunta sem relação com as políticas não pode trazer nenhum
 *       trecho (SC-002 = 100%). Para cada um, o relatório mostra a MAIOR similaridade encontrada sem
 *       limiar, que é a margem real até o limiar configurado.</li>
 * </ul>
 */
class RecuperacaoEvalIT extends BaseEvalIT {

    @Autowired
    private BuscaDocumentoPort busca;

    @Autowired
    private VectorStore vetores;

    @Autowired
    private PropriedadesRag rag;

    @Value("${spring.ai.openai.embedding.model}")
    private String modeloEmbedding;

    @Test
    void trechoEsperadoDeveEstarEntreOsTopKESemRelacaoNaoDeveTrazerNada() {
        List<JsonNode> casos = casos("recuperacao.jsonl");
        StringBuilder relatorio = new StringBuilder("# Eval de recuperação\n\n")
                .append("- modelo de embedding: ").append(modeloEmbedding).append('\n')
                .append("- top-k: ").append(rag.topK()).append(" | limiar: ").append(rag.limiarSimilaridade()).append("\n\n")
                .append("| caso | tipo | resultado | posição | similaridade |\n|---|---|---|---|---|\n");
        int positivos = 0;
        int acertosPositivos = 0;
        int negativos = 0;
        int acertosNegativos = 0;
        double menorPositivo = Double.MAX_VALUE;
        double maiorNegativo = -1;
        for (JsonNode caso : casos) {
            String pergunta = caso.get("pergunta").asString();
            List<TrechoDocumento> trechos = busca.buscar(pergunta);
            if (caso.get("titulo").isNull()) {
                negativos++;
                boolean vazio = trechos.isEmpty();
                if (vazio) {
                    acertosNegativos++;
                }
                double maior = maiorSimilaridadeSemLimiar(pergunta);
                maiorNegativo = Math.max(maiorNegativo, maior);
                relatorio.append("| ").append(caso.get("id").asString()).append(" | negativo | ")
                        .append(vazio ? "recusou" : "TROUXE TRECHO").append(" | - | ")
                        .append(String.format("%.3f", maior)).append(" (maior sem limiar) |\n");
                continue;
            }
            positivos++;
            int posicao = -1;
            for (int i = 0; i < trechos.size(); i++) {
                if (trechos.get(i).titulo().equals(caso.get("titulo").asString())
                        && trechos.get(i).secao().equals(caso.get("secao").asString())) {
                    posicao = i + 1;
                    break;
                }
            }
            if (posicao > 0) {
                acertosPositivos++;
                menorPositivo = Math.min(menorPositivo, trechos.get(posicao - 1).similaridade());
            }
            relatorio.append("| ").append(caso.get("id").asString()).append(" | positivo | ")
                    .append(posicao > 0 ? "acertou" : "ERROU").append(" | ").append(posicao > 0 ? posicao : "-").append(" | ")
                    .append(posicao > 0 ? String.format("%.3f", trechos.get(posicao - 1).similaridade()) : "-").append(" |\n");
        }
        double notaPositivos = (double) acertosPositivos / positivos;
        double notaNegativos = (double) acertosNegativos / negativos;
        relatorio.append("\n**Positivos: ").append(String.format("%.0f%%", notaPositivos * 100)).append("** (mínima 90%) | ")
                .append("**Negativos: ").append(String.format("%.0f%%", notaNegativos * 100)).append("** (mínima 100%)\n\n")
                .append("Margem do limiar ").append(rag.limiarSimilaridade()).append(": menor positivo acertado = ")
                .append(String.format("%.3f", menorPositivo)).append(", maior negativo = ")
                .append(String.format("%.3f", maiorNegativo)).append(". Um limiar entre os dois separa os grupos.\n");
        gravarRelatorio("recuperacao", modeloEmbedding, relatorio.toString());
        assertThat(notaPositivos).as("recuperação dos positivos (SC-003)").isGreaterThanOrEqualTo(0.90);
        assertThat(notaNegativos).as("recusa dos negativos (SC-002)").isEqualTo(1.0);
    }

    /** Maior similaridade entre TODOS os trechos da base (inclusive não autorizados), sem limiar: a margem até o limiar. */
    private double maiorSimilaridadeSemLimiar(String pergunta) {
        List<Document> resultado = vetores.similaritySearch(SearchRequest.builder()
                .query(pergunta).topK(1).similarityThresholdAll().build());
        return resultado.isEmpty() || resultado.getFirst().getScore() == null ? 0.0 : resultado.getFirst().getScore();
    }
}
