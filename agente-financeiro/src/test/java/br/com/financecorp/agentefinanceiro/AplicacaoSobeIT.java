package br.com.financecorp.agentefinanceiro;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.ai.openai.api-key=chave-falsa-de-teste",
            "spring.ai.openai.chat.model=modelo-de-teste",
            // Nenhum teste fala com o provedor real; a porta 9 (discard) recusa qualquer chamada acidental.
            "spring.ai.openai.base-url=http://127.0.0.1:9"
        })
class AplicacaoSobeIT {

    private final HttpClient http = HttpClient.newHttpClient();

    @Autowired
    private Environment ambiente;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void postgres(DynamicPropertyRegistry registro) {
        ContainersTeste.registrarPostgres(registro);
    }

    @Test
    void healthDeveEstarUp() throws Exception {
        HttpResponse<String> resposta = get("/actuator/health");
        assertThat(resposta.statusCode()).isEqualTo(200);
        assertThat(resposta.body()).contains("\"UP\"");
    }

    @Test
    void migracaoV1DeveCriarOsSchemasSemDarCreateAoPapelDeLeitura() {
        Integer schemas = jdbc.queryForObject(
                "select count(*) from information_schema.schemata where schema_name in ('financeiro', 'conhecimento')",
                Integer.class);
        assertThat(schemas).isEqualTo(2);
        assertThat(jdbc.queryForObject("select current_user", String.class)).isEqualTo("agente_leitura");
        assertThat(jdbc.queryForObject("select has_schema_privilege('financeiro', 'CREATE')", Boolean.class)).isFalse();
        assertThat(jdbc.queryForObject("select has_schema_privilege('conhecimento', 'CREATE')", Boolean.class)).isFalse();
        assertThat(jdbc.queryForObject("select count(*) from pg_extension where extname = 'vector'", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void deveEscutarSoNaPropriaMaquina() {
        assertThat(ambiente.getProperty("server.address")).isEqualTo("127.0.0.1");
    }

    @Test
    void rotaInexistenteDeveSer404EmProblemDetail() throws Exception {
        HttpResponse<String> resposta = get("/api/v1/inexistente");
        assertThat(resposta.statusCode()).isEqualTo(404);
        assertThat(resposta.headers().firstValue("Content-Type")).hasValueSatisfying(
                tipo -> assertThat(tipo).startsWith("application/problem+json"));
        assertThat(resposta.body()).doesNotContain("inexistente");
        assertThat(resposta.headers().firstValue("X-Trace-Id")).isPresent();
    }

    @Test
    void swaggerDevePublicarOEndpointDePerguntas() throws Exception {
        HttpResponse<String> docs = get("/v3/api-docs");
        assertThat(docs.statusCode()).isEqualTo(200);
        assertThat(docs.body()).contains("/api/v1/conversas/{conversaId}/perguntas")
                .contains("\"title\":\"AgenteFinanceiro - API REST\"")
                .doesNotContain("/actuator");
        assertThat(get("/swagger-ui/index.html").statusCode()).isEqualTo(200);
    }

    @Test
    void perguntaComChaveRecusadaPeloProvedorDeveSer503SemDetalhe() throws Exception {
        // base-url aponta para a porta 9 (nada escuta): a chamada falha como indisponibilidade do provedor.
        String porta = ambiente.getProperty("local.server.port");
        HttpResponse<String> resposta = http.send(HttpRequest.newBuilder(URI.create(
                        "http://127.0.0.1:" + porta + "/api/v1/conversas/" + java.util.UUID.randomUUID() + "/perguntas"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"pergunta\":\"Qual o limite de hospedagem?\"}"))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertThat(resposta.statusCode()).isEqualTo(503);
        assertThat(resposta.headers().firstValue("Retry-After")).isPresent();
        assertThat(resposta.body()).doesNotContain("127.0.0.1").doesNotContain("Exception").doesNotContain("chave-falsa");
    }

    @Test
    void registroDeExecucaoDeveTerRetencaoDe90Dias() {
        LoggerContext contexto = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger execucao = contexto.getLogger("execucao");
        var appender = (RollingFileAppender<?>) execucao.getAppender("EXECUCAO");
        assertThat(appender).isNotNull();
        assertThat(execucao.isAdditive()).isFalse();
        assertThat(((TimeBasedRollingPolicy<?>) appender.getRollingPolicy()).getMaxHistory()).isEqualTo(90);
    }

    private HttpResponse<String> get(String caminho) throws Exception {
        String porta = ambiente.getProperty("local.server.port");
        HttpRequest requisicao = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + porta + caminho)).GET().build();
        return http.send(requisicao, HttpResponse.BodyHandlers.ofString());
    }
}
