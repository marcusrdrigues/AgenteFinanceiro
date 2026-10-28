package br.com.financecorp.agentefinanceiro.infraestrutura.ia;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.financecorp.agentefinanceiro.ContainersTeste;
import br.com.financecorp.agentefinanceiro.aplicacao.excecao.SaidaModeloInvalidaException;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.ClassificacaoBruta;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.ContextoMontado;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.TrechoRotulado;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.TrechoDocumento;
import br.com.financecorp.agentefinanceiro.dominio.excecao.ModeloIndisponivelException;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Adaptadores OpenAI contra um servidor HTTP falso (testes-ia §7). Confere o número real de
 * requisições: o retry acontece só no ExecutorChamadaModelo (max-retries do SDK = 0).
 */
@SpringBootTest(properties = {
    "spring.ai.openai.api-key=chave-falsa-de-teste",
    "spring.ai.openai.chat.model=modelo-de-teste",
    "spring.ai.openai.timeout=1s",
    "financeiro.ia.backoff-inicial=10ms"
})
class OpenAiAdaptadoresIT {

    private static final WireMockServer OPENAI = new WireMockServer(options().dynamicPort());

    static {
        OPENAI.start();
    }

    @Autowired
    private OpenAiClassificadorAdapter classificador;

    @Autowired
    private OpenAiRedatorDocumentalAdapter redator;

    @DynamicPropertySource
    static void propriedades(DynamicPropertyRegistry registro) {
        ContainersTeste.registrarPostgres(registro);
        registro.add("spring.ai.openai.base-url", OPENAI::baseUrl);
    }

    @AfterAll
    static void parar() {
        OPENAI.stop();
    }

    @BeforeEach
    void limpar() {
        OPENAI.resetAll();
    }

    @Test
    void respostaValidaDeveSerConvertida() {
        responder(200, conclusao("{\\\"tipo\\\":\\\"DOCUMENTAL\\\",\\\"perguntaAutonoma\\\":\\\"Qual o limite?\\\","
                + "\\\"inicio\\\":\\\"\\\",\\\"fim\\\":\\\"\\\",\\\"categoria\\\":\\\"\\\"}"));

        ClassificacaoBruta c = classificador.classificar("Qual o limite?", List.of(), Optional.empty());

        assertThat(c.tipo()).isEqualTo("DOCUMENTAL");
        assertThat(c.perguntaAutonoma()).isEqualTo("Qual o limite?");
        OPENAI.verify(1, postRequestedFor(urlPathMatching(".*/chat/completions")));
    }

    @Test
    void erro429DeveSerRepetidoEDepoisVirarIndisponivel() {
        responder(429, "{\"error\":{\"message\":\"limite\",\"type\":\"requests\"}}");

        assertThatThrownBy(() -> classificador.classificar("x", List.of(), Optional.empty()))
                .isInstanceOf(ModeloIndisponivelException.class);
        OPENAI.verify(3, anyRequestedFor(anyUrl()));
    }

    @Test
    void erro500DeveSerRepetidoEDepoisVirarIndisponivel() {
        responder(500, "{\"error\":{\"message\":\"falha\",\"type\":\"server_error\"}}");

        assertThatThrownBy(() -> redator.redigir("x", contexto(), Optional.empty()))
                .isInstanceOf(ModeloIndisponivelException.class);
        OPENAI.verify(3, anyRequestedFor(anyUrl()));
    }

    @Test
    void timeoutDeveSerRepetidoEDepoisVirarIndisponivel() {
        OPENAI.stubFor(post(anyUrl()).willReturn(aResponse().withStatus(200).withFixedDelay(2_000)
                .withHeader("Content-Type", "application/json").withBody(conclusao("{}"))));

        assertThatThrownBy(() -> classificador.classificar("x", List.of(), Optional.empty()))
                .isInstanceOf(ModeloIndisponivelException.class);
        OPENAI.verify(3, anyRequestedFor(anyUrl()));
    }

    @Test
    void erro401NaoDeveSerRepetido() {
        responder(401, "{\"error\":{\"message\":\"chave inválida\",\"type\":\"invalid_request_error\"}}");

        assertThatThrownBy(() -> classificador.classificar("x", List.of(), Optional.empty()))
                .isInstanceOf(ModeloIndisponivelException.class)
                .hasMessageNotContaining("chave-falsa-de-teste");
        OPENAI.verify(1, anyRequestedFor(anyUrl()));
    }

    @Test
    void saidaForaDoSchemaDeveVirarSaidaInvalidaSemRepetirNoExecutor() {
        responder(200, conclusao("isto nao e json"));

        assertThatThrownBy(() -> redator.redigir("x", contexto(), Optional.empty()))
                .isInstanceOf(SaidaModeloInvalidaException.class);
        OPENAI.verify(1, anyRequestedFor(anyUrl()));
    }

    @Test
    void requisicaoNaoDeveLevarFerramentasNemAChaveNoCorpo() {
        responder(200, conclusao("{\\\"resposta\\\":\\\"ok\\\",\\\"conhecimentoSuficiente\\\":false,\\\"referencias\\\":[]}"));

        redator.redigir("Qual o prazo?", contexto(), Optional.empty());

        String corpo = OPENAI.getAllServeEvents().getFirst().getRequest().getBodyAsString();
        assertThat(corpo).doesNotContain("\"tools\"").doesNotContain("chave-falsa-de-teste");
        assertThat(corpo).contains("max_completion_tokens");
    }

    private static void responder(int status, String corpo) {
        OPENAI.stubFor(post(anyUrl()).willReturn(aResponse().withStatus(status)
                .withHeader("Content-Type", "application/json").withBody(corpo)));
    }

    /** Resposta mínima do endpoint chat/completions; {@code conteudo} já vem escapado para JSON. */
    private static String conclusao(String conteudo) {
        return "{\"id\":\"chatcmpl-teste\",\"object\":\"chat.completion\",\"created\":1700000000,"
                + "\"model\":\"modelo-de-teste\",\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\","
                + "\"content\":\"" + conteudo + "\",\"refusal\":null},\"logprobs\":null,\"finish_reason\":\"stop\"}],"
                + "\"usage\":{\"prompt_tokens\":10,\"completion_tokens\":5,\"total_tokens\":15}}";
    }

    private static ContextoMontado contexto() {
        TrechoDocumento t = new TrechoDocumento(UUID.randomUUID(), "Política", "v1", "Seção", "Prazo de 30 dias.", 0.9);
        return new ContextoMontado(List.of(new TrechoRotulado("T1", t)), "<trechos>\n...\n</trechos>");
    }
}
