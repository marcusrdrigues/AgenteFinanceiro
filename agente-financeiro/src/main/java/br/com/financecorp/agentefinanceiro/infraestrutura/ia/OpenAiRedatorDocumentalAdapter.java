package br.com.financecorp.agentefinanceiro.infraestrutura.ia;

import br.com.financecorp.agentefinanceiro.aplicacao.contexto.Neutralizador;
import br.com.financecorp.agentefinanceiro.aplicacao.excecao.SaidaModeloInvalidaException;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.ContextoMontado;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.RespostaDocumentalRedigida;
import br.com.financecorp.agentefinanceiro.aplicacao.porta.RedatorDocumentalPort;
import br.com.financecorp.agentefinanceiro.dominio.excecao.ModeloIndisponivelException;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Optional;
import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Redação da resposta documental a partir do bloco {@code <trechos>} montado em código. Sem
 * ferramentas: a busca já foi feita pela aplicação (spring-ai §1). O record de saída do modelo não
 * atravessa a porta.
 */
@Component
public final class OpenAiRedatorDocumentalAdapter implements RedatorDocumentalPort {

    static final String PROMPT = "responder-documental";

    record RespostaDocumentalModelo(
            @JsonProperty(required = true) String resposta,
            @JsonProperty(required = true) boolean conhecimentoSuficiente,
            @JsonProperty(required = true) List<String> referencias) {
    }

    private final ChatClient cliente;
    private final ExecutorChamadaModelo executor;
    private final Resource system;
    private final Resource user;
    private final int maximoTokens;

    public OpenAiRedatorDocumentalAdapter(ChatClient.Builder construtor, ExecutorChamadaModelo executor,
            CarregadorPrompts prompts, @Value("${financeiro.ia.tokens-redacao:600}") int maximoTokens) {
        this.cliente = construtor.build();
        this.executor = executor;
        this.system = prompts.system(PROMPT);
        this.user = prompts.user(PROMPT);
        this.maximoTokens = maximoTokens;
    }

    @Override
    public RespostaDocumentalRedigida redigir(String pergunta, ContextoMontado contexto, Optional<String> correcao) {
        RespostaDocumentalModelo saida;
        try {
            saida = executor.executar(() -> cliente.prompt()
                    .system(system)
                    .user(u -> u.text(user)
                            // O bloco já vem neutralizado do MontadorContexto; entra como um único parâmetro.
                            .param("trechos", contexto.bloco())
                            .param("pergunta", Neutralizador.neutralizar(pergunta))
                            .param("correcao", correcao.map(Neutralizador::neutralizar).orElse("nenhuma")))
                    .options(OpenAiChatOptions.builder().maxCompletionTokens(maximoTokens))
                    .advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                    .call()
                    .entity(RespostaDocumentalModelo.class));
        } catch (ModeloIndisponivelException indisponivel) {
            throw indisponivel;
        } catch (RuntimeException conversao) {
            throw new SaidaModeloInvalidaException("a saída não é um JSON válido no formato pedido");
        }
        if (saida == null) {
            throw new SaidaModeloInvalidaException("a saída veio vazia");
        }
        return new RespostaDocumentalRedigida(saida.resposta(), saida.conhecimentoSuficiente(), saida.referencias());
    }
}
