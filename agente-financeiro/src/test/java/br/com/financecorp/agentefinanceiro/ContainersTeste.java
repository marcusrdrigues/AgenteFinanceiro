package br.com.financecorp.agentefinanceiro;

import java.nio.file.Path;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Postgres + pgvector real, com o mesmo script de papéis do docker-compose. Um container por
 * execução da suíte (iniciado na primeira classe que usar).
 *
 * Desvio consciente de {@code @ServiceConnection}: ele trocaria as credenciais da aplicação pelas
 * do superusuário do container e esconderia justamente os erros de permissão dos papéis
 * agente_leitura e agente_ingestao. Por isso as propriedades são registradas à mão.
 */
public final class ContainersTeste {

    public static final String SENHA_MIGRACAO = "migracao-teste";
    public static final String SENHA_INGESTAO = "ingestao-teste";
    public static final String SENHA_LEITURA = "leitura-teste";

    public static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("pgvector/pgvector:0.8.7-pg18").asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("financecorp")
            .withUsername("postgres")
            .withPassword("superusuario-teste")
            .withEnv("POSTGRES_MIGRACAO_PASSWORD", SENHA_MIGRACAO)
            .withEnv("POSTGRES_INGESTAO_PASSWORD", SENHA_INGESTAO)
            .withEnv("POSTGRES_APP_PASSWORD", SENHA_LEITURA)
            // Caminho relativo ao diretório do módulo, onde o Maven roda os testes.
            .withCopyFileToContainer(
                    MountableFile.forHostPath(Path.of("../infra/postgres/init/01-papeis.sh"), 0755),
                    "/docker-entrypoint-initdb.d/01-papeis.sh");

    private ContainersTeste() {
    }

    public static void registrarPostgres(DynamicPropertyRegistry registro) {
        if (!POSTGRES.isRunning()) {
            POSTGRES.start();
        }
        registro.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registro.add("spring.datasource.username", () -> "agente_leitura");
        registro.add("spring.datasource.password", () -> SENHA_LEITURA);
        registro.add("spring.flyway.user", () -> "agente_migracao");
        registro.add("spring.flyway.password", () -> SENHA_MIGRACAO);
    }
}
