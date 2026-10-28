package br.com.financecorp.agentefinanceiro.eval;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.financecorp.agentefinanceiro.aplicacao.modelo.ClassificacaoBruta;
import br.com.financecorp.agentefinanceiro.aplicacao.porta.ClassificadorPerguntaPort;
import br.com.financecorp.agentefinanceiro.aplicacao.validacao.ValidadorClassificacao;
import br.com.financecorp.agentefinanceiro.aplicacao.validacao.ValidadorClassificacao.Resultado;
import br.com.financecorp.agentefinanceiro.infraestrutura.config.PropriedadesIa;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import tools.jackson.databind.JsonNode;

/**
 * Eval da classificação (SC-010 ≥ 95%), corrigido por código. Cada pergunta é classificada
 * {@value #REPETICOES} vezes: temperature 0 não garante saída idêntica, e uma fronteira instável entre
 * tipos só aparece repetindo. A nota é a fração de chamadas corretas, sem passar pelo caminho de nova
 * tentativa (mede o prompt, não a rede de segurança).
 */
class ClassificacaoEvalIT extends BaseEvalIT {

    private static final int REPETICOES = 3;
    private static final double NOTA_MINIMA = 0.95;

    @Autowired
    private ClassificadorPerguntaPort classificador;

    @Autowired
    private PropriedadesIa ia;

    @Value("${spring.ai.openai.chat.model}")
    private String modelo;

    @Test
    void perguntaDeveSerClassificadaNoTipoEsperado() {
        ValidadorClassificacao validador = new ValidadorClassificacao(1000);
        List<JsonNode> casos = casos("classificacao.jsonl");
        StringBuilder relatorio = new StringBuilder("# Eval de classificação\n\n")
                .append("- modelo: ").append(modelo).append("\n- prompts: ").append(ia.versaoPrompts()).append("\n- repetições por caso: ")
                .append(REPETICOES).append("\n\n| caso | esperado | obtido | acertos |\n|---|---|---|---|\n");
        int acertos = 0;
        for (JsonNode caso : casos) {
            String esperado = caso.get("tipo").asString();
            int certos = 0;
            StringBuilder obtidos = new StringBuilder();
            for (int i = 0; i < REPETICOES; i++) {
                String obtido = classificar(validador, caso.get("pergunta").asString());
                obtidos.append(i == 0 ? "" : ", ").append(obtido);
                if (obtido.equals(esperado)) {
                    certos++;
                }
            }
            acertos += certos;
            relatorio.append("| ").append(caso.get("id").asString()).append(" | ").append(esperado).append(" | ")
                    .append(obtidos).append(" | ").append(certos).append('/').append(REPETICOES).append(" |\n");
        }
        double nota = (double) acertos / (casos.size() * REPETICOES);
        relatorio.append("\n**Nota: ").append(String.format("%.0f%%", nota * 100)).append("** (mínima 95%)\n");
        gravarRelatorio("classificacao", modelo + "-" + ia.versaoPrompts().get("classificar"), relatorio.toString());
        assertThat(nota).as("classificação (SC-010)").isGreaterThanOrEqualTo(NOTA_MINIMA);
    }

    private String classificar(ValidadorClassificacao validador, String pergunta) {
        try {
            ClassificacaoBruta bruta = classificador.classificar(pergunta, List.of(), Optional.empty());
            return switch (validador.validar(bruta)) {
                case Resultado.Valida valida -> valida.classificacao().tipo().name();
                case Resultado.Invalida invalida -> "INVALIDA";
            };
        } catch (RuntimeException falha) {
            return "ERRO";
        }
    }
}
