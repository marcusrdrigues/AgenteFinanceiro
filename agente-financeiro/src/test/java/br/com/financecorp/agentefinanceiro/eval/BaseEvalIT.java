package br.com.financecorp.agentefinanceiro.eval;

import br.com.financecorp.agentefinanceiro.ContainersTeste;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Eval com o modelo REAL (testes-ia §9): fora do build padrão, roda com {@code -Peval}. Exige
 * OPENAI_API_KEY e OPENAI_CHAT_MODEL no ambiente; sem eles falha com mensagem clara, em vez de pular.
 * Indexa o conjunto curado de documentos/ (perfil ingestao) num Postgres de Testcontainers.
 */
@Tag("eval")
@SpringBootTest
@ActiveProfiles("ingestao")
abstract class BaseEvalIT {

    protected static final JsonMapper JSON = JsonMapper.builder().build();

    @BeforeAll
    static void exigirChave() {
        for (String variavel : List.of("OPENAI_API_KEY", "OPENAI_CHAT_MODEL")) {
            String valor = System.getenv(variavel);
            if (valor == null || valor.isBlank()) {
                throw new IllegalStateException("O eval chama o modelo real: defina " + variavel + " no ambiente.");
            }
        }
    }

    @DynamicPropertySource
    static void propriedades(DynamicPropertyRegistry registro) {
        ContainersTeste.registrarPostgres(registro);
        registro.add("spring.datasource.username", () -> "agente_ingestao");
        registro.add("spring.datasource.password", () -> ContainersTeste.SENHA_INGESTAO);
        registro.add("financeiro.ingestao.pasta", () -> "documentos");
    }

    protected static List<JsonNode> casos(String arquivo) {
        try {
            return Files.readAllLines(Path.of("eval", arquivo), StandardCharsets.UTF_8).stream()
                    .filter(l -> !l.isBlank())
                    .map(JSON::readTree)
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Grava o relatório em eval/resultados/ para comparar com a execução anterior. */
    protected static void gravarRelatorio(String conjunto, String modelo, String conteudo) {
        try {
            Path pasta = Files.createDirectories(Path.of("eval", "resultados"));
            String nome = LocalDate.now() + "-" + modelo.replaceAll("[^A-Za-z0-9.-]", "_") + "-" + conjunto + ".md";
            Files.writeString(pasta.resolve(nome), conteudo, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
