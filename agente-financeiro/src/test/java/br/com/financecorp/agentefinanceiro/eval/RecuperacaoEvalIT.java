package br.com.financecorp.agentefinanceiro.eval;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.financecorp.agentefinanceiro.aplicacao.modelo.TrechoDocumento;
import br.com.financecorp.agentefinanceiro.aplicacao.porta.BuscaDocumentoPort;
import br.com.financecorp.agentefinanceiro.infraestrutura.config.PropriedadesRag;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import tools.jackson.databind.JsonNode;

/** Eval de recuperação, medido separado da resposta: o trecho certo veio entre os top-k? Meta SC-003 ≥ 90%. */
class RecuperacaoEvalIT extends BaseEvalIT {

    private static final double NOTA_MINIMA = 0.90;

    @Autowired
    private BuscaDocumentoPort busca;

    @Autowired
    private PropriedadesRag rag;

    @Value("${spring.ai.openai.embedding.model}")
    private String modeloEmbedding;

    @Test
    void trechoEsperadoDeveEstarEntreOsTopK() {
        List<JsonNode> casos = casos("recuperacao.jsonl");
        StringBuilder relatorio = new StringBuilder("# Eval de recuperação\n\n")
                .append("- modelo de embedding: ").append(modeloEmbedding).append('\n')
                .append("- top-k: ").append(rag.topK()).append(" | limiar: ").append(rag.limiarSimilaridade()).append("\n\n")
                .append("| caso | acertou | posição | similaridade |\n|---|---|---|---|\n");
        int acertos = 0;
        for (JsonNode caso : casos) {
            List<TrechoDocumento> trechos = busca.buscar(caso.get("pergunta").asString());
            int posicao = -1;
            for (int i = 0; i < trechos.size(); i++) {
                if (trechos.get(i).titulo().equals(caso.get("titulo").asString())
                        && trechos.get(i).secao().equals(caso.get("secao").asString())) {
                    posicao = i + 1;
                    break;
                }
            }
            if (posicao > 0) {
                acertos++;
            }
            relatorio.append("| ").append(caso.get("id").asString()).append(" | ").append(posicao > 0 ? "sim" : "não")
                    .append(" | ").append(posicao > 0 ? posicao : "-").append(" | ")
                    .append(posicao > 0 ? String.format("%.3f", trechos.get(posicao - 1).similaridade()) : "-")
                    .append(" |\n");
        }
        double nota = (double) acertos / casos.size();
        relatorio.append("\n**Nota: ").append(String.format("%.0f%%", nota * 100)).append("** (mínima 90%)\n");
        gravarRelatorio("recuperacao", modeloEmbedding, relatorio.toString());
        assertThat(nota).as("recuperação (SC-003)").isGreaterThanOrEqualTo(NOTA_MINIMA);
    }
}
