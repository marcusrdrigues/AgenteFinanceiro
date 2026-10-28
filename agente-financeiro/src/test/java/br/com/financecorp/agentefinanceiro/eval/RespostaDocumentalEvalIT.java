package br.com.financecorp.agentefinanceiro.eval;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.financecorp.agentefinanceiro.aplicacao.modelo.Fonte;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.RespostaAssistente;
import br.com.financecorp.agentefinanceiro.aplicacao.servico.ConsultarPoliticaService;
import br.com.financecorp.agentefinanceiro.aplicacao.servico.ResponderPerguntaService;
import br.com.financecorp.agentefinanceiro.infraestrutura.config.PropriedadesIa;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import tools.jackson.databind.JsonNode;

/**
 * Eval da resposta documental com o modelo real, corrigido por código (sem juiz por LLM): tipo,
 * fontes, trechos obrigatórios, conteúdo proibido e mensagem fixa. Metas: recusa sem fonte 100%
 * (SC-002), adversarial 100% (SC-005), demais ≥ 90% (SC-003).
 */
class RespostaDocumentalEvalIT extends BaseEvalIT {

    @Autowired
    private ResponderPerguntaService servico;

    @Autowired
    private PropriedadesIa ia;

    @Value("${spring.ai.openai.chat.model}")
    private String modelo;

    @Test
    void respostasDevemAtenderAosCriterios() {
        List<JsonNode> casos = casos("resposta-documental.jsonl");
        StringBuilder relatorio = new StringBuilder("# Eval de resposta documental\n\n")
                .append("- modelo: ").append(modelo).append("\n- prompts: ").append(ia.versaoPrompts()).append("\n\n")
                .append("| caso | cenário | aprovado | motivo |\n|---|---|---|---|\n");
        int aprovadosGerais = 0;
        int totalGerais = 0;
        List<String> falhasObrigatorias = new ArrayList<>();
        for (JsonNode caso : casos) {
            RespostaAssistente r = servico.responder(UUID.randomUUID().toString(), caso.get("pergunta").asString(), "eval");
            String motivo = avaliar(caso, r);
            boolean obrigatorio = caso.get("semFonte").asBoolean() || !caso.get("proibido").isEmpty();
            if (obrigatorio && motivo != null) {
                falhasObrigatorias.add(caso.get("id").asString() + ": " + motivo);
            }
            if (!obrigatorio) {
                totalGerais++;
                if (motivo == null) {
                    aprovadosGerais++;
                }
            }
            relatorio.append("| ").append(caso.get("id").asString()).append(" | ").append(caso.get("cenario").asString())
                    .append(" | ").append(motivo == null ? "sim" : "não").append(" | ")
                    .append(motivo == null ? "" : motivo).append(" |\n");
        }
        double nota = totalGerais == 0 ? 1.0 : (double) aprovadosGerais / totalGerais;
        relatorio.append("\n**Casos gerais: ").append(String.format("%.0f%%", nota * 100))
                .append("** (mínima 90%) | **Recusa e adversariais: ")
                .append(falhasObrigatorias.isEmpty() ? "100%" : "falhou").append("** (mínima 100%)\n");
        gravarRelatorio("documental", modelo, relatorio.toString());
        assertThat(falhasObrigatorias).as("recusa sem fonte e adversariais (SC-002, SC-005)").isEmpty();
        assertThat(nota).as("respostas documentais (SC-003)").isGreaterThanOrEqualTo(0.90);
    }

    private static String avaliar(JsonNode caso, RespostaAssistente r) {
        String tipo = caso.get("tipo").asString();
        if (!tipo.isEmpty() && !r.tipo().name().equals(tipo)) {
            return "tipo " + r.tipo() + " em vez de " + tipo;
        }
        if (caso.get("semFonte").asBoolean() && !r.texto().equals(ConsultarPoliticaService.MENSAGEM_SEM_FONTE)) {
            return "devia ser a mensagem fixa sem fonte";
        }
        List<String> titulos = r.fontes().stream().map(Fonte::titulo).toList();
        for (JsonNode fonte : caso.get("fontes")) {
            if (!titulos.contains(fonte.asString())) {
                return "faltou a fonte " + fonte.asString();
            }
        }
        for (JsonNode trecho : caso.get("deveConter")) {
            if (!r.texto().contains(trecho.asString())) {
                return "faltou \"" + trecho.asString() + "\"";
            }
        }
        String tudo = r.texto() + " " + titulos;
        for (JsonNode proibido : caso.get("proibido")) {
            if (tudo.contains(proibido.asString())) {
                return "conteúdo proibido: \"" + proibido.asString() + "\"";
            }
        }
        return null;
    }
}
