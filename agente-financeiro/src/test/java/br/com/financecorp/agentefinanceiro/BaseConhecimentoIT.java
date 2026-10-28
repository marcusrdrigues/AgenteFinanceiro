package br.com.financecorp.agentefinanceiro;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Contexto comum dos testes da base de políticas: Postgres + pgvector real, migrações reais,
 * perfil ingestao (papel agente_ingestao) e embedding falso. Um único contexto Spring para todas as
 * subclasses.
 */
@SpringBootTest(properties = {
    "spring.ai.openai.api-key=chave-falsa-de-teste",
    "spring.ai.openai.chat.model=modelo-de-teste",
    "spring.ai.openai.base-url=http://127.0.0.1:9",
    "spring.ai.openai.embedding.model=embedding-falso-v1",
    "spring.ai.model.embedding=none",
    "financeiro.rag.lote-gravacao=2"
})
@ActiveProfiles("ingestao")
@Import(BaseConhecimentoIT.ConfiguracaoTeste.class)
public abstract class BaseConhecimentoIT {

    protected static final Path PASTA_DOCUMENTOS = pastaTemporaria();

    @Autowired
    protected EmbeddingFalso embedding;

    @DynamicPropertySource
    static void propriedades(DynamicPropertyRegistry registro) {
        ContainersTeste.registrarPostgres(registro);
        registro.add("spring.datasource.username", () -> "agente_ingestao");
        registro.add("spring.datasource.password", () -> ContainersTeste.SENHA_INGESTAO);
        registro.add("financeiro.ingestao.pasta", PASTA_DOCUMENTOS::toString);
    }

    @BeforeEach
    void limparBase() throws IOException {
        embedding.restaurar();
        superusuario().execute("DELETE FROM conhecimento.vector_store; DELETE FROM conhecimento.documento;");
        try (var arquivos = Files.list(PASTA_DOCUMENTOS)) {
            for (Path p : arquivos.toList()) {
                Files.delete(p);
            }
        }
    }

    /** Conexão de administração, só para preparar e inspecionar o banco no teste. */
    protected static JdbcTemplate superusuario() {
        return new JdbcTemplate(new DriverManagerDataSource(ContainersTeste.POSTGRES.getJdbcUrl(),
                ContainersTeste.POSTGRES.getUsername(), ContainersTeste.POSTGRES.getPassword()));
    }

    protected static void escreverDocumento(String arquivo, String conteudo) {
        try {
            Files.writeString(PASTA_DOCUMENTOS.resolve(arquivo), conteudo);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    protected static String politica(String titulo, String versao, String situacao, String corpo) {
        return "---\ntitulo: " + titulo + "\nversao: " + versao + "\nvigenteDesde: 2025-01-01\nsituacao: " + situacao
                + "\n---\n# " + titulo + "\n" + corpo + "\n";
    }

    private static Path pastaTemporaria() {
        try {
            return Files.createTempDirectory("documentos-teste");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @TestConfiguration
    static class ConfiguracaoTeste {
        @Bean
        EmbeddingFalso embeddingFalso() {
            return new EmbeddingFalso();
        }
    }
}
