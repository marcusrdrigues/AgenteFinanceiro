package br.com.financecorp.agentefinanceiro.infraestrutura.ia;

import br.com.financecorp.agentefinanceiro.aplicacao.contexto.Neutralizador;
import br.com.financecorp.agentefinanceiro.aplicacao.excecao.SaidaModeloInvalidaException;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.ClassificacaoBruta;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.Turno;
import br.com.financecorp.agentefinanceiro.aplicacao.porta.ConsumoModeloPort;
import br.com.financecorp.agentefinanceiro.aplicacao.porta.ClassificadorPerguntaPort;
import br.com.financecorp.agentefinanceiro.dominio.excecao.ModeloIndisponivelException;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Classificação e reescrita autônoma da pergunta. {@code ChatClient} próprio e sem ferramentas: o
 * classificador só devolve dados, não age. Saída estruturada nativa da OpenAI (research D5).
 */
@Component
public final class OpenAiClassificadorAdapter implements ClassificadorPerguntaPort {

    static final String PROMPT = "classificar";

    /** Saída do modelo. Campos vazios ("") significam "não informado"; a validação fica na aplicação. */
    record ClassificacaoModelo(
            @JsonProperty(required = true) String tipo,
            @JsonProperty(required = true) String perguntaAutonoma,
            @JsonProperty(required = true) String inicio,
            @JsonProperty(required = true) String fim,
            @JsonProperty(required = true) String categoria) {
    }

    private final ChatClient cliente;
    private final ExecutorChamadaModelo executor;
    private final ConsumoModeloPort consumo;
    private final Resource system;
    private final Resource user;
    private final int maximoTokens;

    public OpenAiClassificadorAdapter(ChatClient.Builder construtor, ExecutorChamadaModelo executor, ConsumoModeloPort consumo,
            CarregadorPrompts prompts, @Value("${financeiro.ia.tokens-classificador:200}") int maximoTokens) {
        this.cliente = construtor.build();
        this.executor = executor;
        this.consumo = consumo;
        this.system = prompts.system(PROMPT);
        this.user = prompts.user(PROMPT);
        this.maximoTokens = maximoTokens;
    }

    @Override
    public ClassificacaoBruta classificar(String pergunta, List<Turno> historico, Optional<String> correcao) {
        ClassificacaoModelo saida;
        try {
            var resposta = executor.executar(() -> cliente.prompt()
                    .system(system)
                    .user(u -> u.text(user)
                            .param("historico", historico(historico))
                            .param("pergunta", Neutralizador.neutralizar(pergunta))
                            .param("correcao", correcao.map(Neutralizador::neutralizar).orElse("nenhuma")))
                    .options(OpenAiChatOptions.builder().maxCompletionTokens(maximoTokens))
                    .advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                    .call()
                    .responseEntity(ClassificacaoModelo.class));
            UsoModelo.registrar(consumo, resposta.response());
            saida = resposta.entity();
        } catch (ModeloIndisponivelException indisponivel) {
            throw indisponivel;
        } catch (RuntimeException conversao) {
            throw new SaidaModeloInvalidaException("a saída não é um JSON válido no formato pedido");
        }
        if (saida == null) {
            throw new SaidaModeloInvalidaException("a saída veio vazia");
        }
        return new ClassificacaoBruta(saida.tipo(), saida.perguntaAutonoma(), saida.inicio(), saida.fim(),
                saida.categoria());
    }

    private static String historico(List<Turno> historico) {
        if (historico.isEmpty()) {
            return "(sem histórico)";
        }
        return historico.stream()
                .map(t -> (t.papel() == Turno.Papel.USUARIO ? "usuario: " : "assistente: ")
                        + Neutralizador.neutralizar(t.texto()))
                .collect(Collectors.joining("\n"));
    }
}
