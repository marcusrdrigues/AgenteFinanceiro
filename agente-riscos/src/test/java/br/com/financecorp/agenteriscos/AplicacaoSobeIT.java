package br.com.financecorp.agenteriscos;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

/** Sobe sem nenhuma variável de modelo: o agente-riscos não usa LLM. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AplicacaoSobeIT {

    @Autowired
    private Environment ambiente;

    @Test
    void healthDeveEstarUp() throws Exception {
        HttpResponse<String> resposta = get("/actuator/health");
        assertThat(resposta.statusCode()).isEqualTo(200);
        assertThat(resposta.body()).contains("\"UP\"");
    }

    @Test
    void swaggerDevePublicarADocumentacao() throws Exception {
        HttpResponse<String> docs = get("/v3/api-docs");
        assertThat(docs.statusCode()).isEqualTo(200);
        assertThat(docs.body()).contains("\"title\":\"agente-riscos\"").contains("A2A");
        assertThat(get("/swagger-ui/index.html").statusCode()).isEqualTo(200);
    }

    private HttpResponse<String> get(String caminho) throws Exception {
        String porta = ambiente.getProperty("local.server.port");
        return HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + porta + caminho)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void deveEscutarSoNaPropriaMaquina() {
        assertThat(ambiente.getProperty("server.address")).isEqualTo("127.0.0.1");
    }

    @Test
    void naoDeveTerNenhumaPropriedadeDeModelo() {
        assertThat(ambiente.getProperty("spring.ai.openai.api-key")).isNull();
    }
}
