package br.com.financecorp.agentefinanceiro.infraestrutura.persistencia;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.financecorp.agentefinanceiro.BaseConhecimentoIT;
import br.com.financecorp.agentefinanceiro.ContainersTeste;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/** Menor privilégio no banco (constituição VII): cada papel só faz o que a função dele exige. */
class PapeisBancoIT extends BaseConhecimentoIT {

    private static final String INSERIR_DOCUMENTO = """
            INSERT INTO conhecimento.documento (id, titulo, versao, vigente_desde, situacao, hash_conteudo, texto,
                modelo_embedding, arquivo_origem, cadastrado_por, cadastrado_em)
            VALUES (gen_random_uuid(), 'Teste', 'v1', DATE '2025-01-01', 'AUTORIZADO', repeat('a', 64), 'x',
                'modelo', 'teste.md', 'teste', now())
            """;

    @Test
    void papelDeLeituraNaoEscreveNaBaseDePoliticas() {
        assertThatThrownBy(() -> como("agente_leitura", ContainersTeste.SENHA_LEITURA).update(INSERIR_DOCUMENTO))
                .rootCause().hasMessageContaining("permission denied");
        assertThatCode(() -> como("agente_leitura", ContainersTeste.SENHA_LEITURA)
                .queryForObject("SELECT count(*) FROM conhecimento.vector_store", Integer.class))
                .doesNotThrowAnyException();
    }

    @Test
    void papelDeIngestaoEscreveNaBaseDePoliticasMasNaoEnxergaFinanceiro() {
        assertThatCode(() -> como("agente_ingestao", ContainersTeste.SENHA_INGESTAO).update(INSERIR_DOCUMENTO))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> como("agente_ingestao", ContainersTeste.SENHA_INGESTAO)
                .queryForObject("SELECT count(*) FROM financeiro.flyway_schema_history", Integer.class))
                .rootCause().hasMessageContaining("permission denied");
    }

    @Test
    void nenhumPapelDaAplicacaoCriaTabela() {
        assertThatThrownBy(() -> como("agente_leitura", ContainersTeste.SENHA_LEITURA)
                .execute("CREATE TABLE conhecimento.invasora (id int)")).rootCause().hasMessageContaining("permission denied");
        assertThatThrownBy(() -> como("agente_ingestao", ContainersTeste.SENHA_INGESTAO)
                .execute("CREATE TABLE conhecimento.invasora (id int)")).rootCause().hasMessageContaining("permission denied");
    }

    private static JdbcTemplate como(String papel, String senha) {
        return new JdbcTemplate(new DriverManagerDataSource(ContainersTeste.POSTGRES.getJdbcUrl(), papel, senha));
    }
}
