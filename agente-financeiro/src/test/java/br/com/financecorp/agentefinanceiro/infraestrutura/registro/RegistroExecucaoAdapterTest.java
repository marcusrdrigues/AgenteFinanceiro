package br.com.financecorp.agentefinanceiro.infraestrutura.registro;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.financecorp.agentefinanceiro.aplicacao.modelo.RegistroExecucao;
import br.com.financecorp.agentefinanceiro.dominio.TipoResposta;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class RegistroExecucaoAdapterTest {

    private final JsonMapper json = JsonMapper.builder().build();
    private final Slf4jRegistroExecucaoAdapter adaptador = new Slf4jRegistroExecucaoAdapter(json);
    private final ListAppender<ILoggingEvent> capturados = new ListAppender<>();
    private Logger logger;

    @BeforeEach
    void capturar() {
        logger = (Logger) LoggerFactory.getLogger(Slf4jRegistroExecucaoAdapter.LOGGER);
        capturados.start();
        logger.addAppender(capturados);
    }

    @AfterEach
    void soltar() {
        logger.detachAppender(capturados);
    }

    @Test
    void deveGravarUmaLinhaJsonComOsMetadados() {
        UUID conversa = UUID.randomUUID();
        UUID solicitacao = UUID.randomUUID();

        adaptador.registrar(new RegistroExecucao("trace-1", conversa, TipoResposta.RISCO_ESPECIALIZADO, 1234L,
                300, 120, Map.of("classificar", "v1"), List.of("Política de Reembolso@v2"),
                List.of("consultarResumoPeriodo:2025-01-01..2025-03-31:-"), solicitacao, "AGENTE_ESPECIALISTA",
                "respondida", null));

        assertThat(capturados.list).hasSize(1);
        String linha = capturados.list.getFirst().getFormattedMessage();
        assertThat(linha).doesNotContain("\n");
        JsonNode no = json.readTree(linha);
        assertThat(no.get("traceId").asString()).isEqualTo("trace-1");
        assertThat(no.get("conversaId").asString()).isEqualTo(conversa.toString());
        assertThat(no.get("tipo").asString()).isEqualTo("RISCO_ESPECIALIZADO");
        assertThat(no.get("duracaoMs").asLong()).isEqualTo(1234L);
        assertThat(no.get("tokensEntrada").asInt()).isEqualTo(300);
        assertThat(no.get("tokensSaida").asInt()).isEqualTo(120);
        assertThat(no.get("versoesPrompt").get("classificar").asString()).isEqualTo("v1");
        assertThat(no.get("documentosConsultados").get(0).asString()).isEqualTo("Política de Reembolso@v2");
        assertThat(no.get("consultasNumericas").get(0).asString()).startsWith("consultarResumoPeriodo:");
        assertThat(no.get("solicitacaoEspecialistaId").asString()).isEqualTo(solicitacao.toString());
        assertThat(no.get("resultado").asString()).isEqualTo("respondida");
        assertThat(no.has("codigoErro")).isTrue();
    }

    @Test
    void registroNaoTemCampoParaTextoDePerguntaRespostaOuValor() {
        List<String> campos = Arrays.stream(RegistroExecucao.class.getRecordComponents())
                .map(RecordComponent::getName).toList();
        // "versoesPrompt" é permitido: guarda só a versão do arquivo de prompt, nunca o conteúdo.
        assertThat(campos).noneMatch(c -> c.matches("(?i).*(pergunta|resposta|texto|valor|conteudo).*"));
        assertThat(campos).contains("versoesPrompt");
    }
}
