package br.com.financecorp.agentefinanceiro.infraestrutura.persistencia;

import br.com.financecorp.agentefinanceiro.aplicacao.modelo.DocumentoParaIngestao;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.DocumentoRegistrado;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.SituacaoDocumento;
import br.com.financecorp.agentefinanceiro.aplicacao.porta.RepositorioDocumentoPort;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Tabela conhecimento.documento. Só parâmetros vinculados (constituição VII). */
@Component
public final class JdbcRepositorioDocumentoAdapter implements RepositorioDocumentoPort {

    private static final String COLUNAS = "id, titulo, versao, arquivo_origem, hash_conteudo, situacao";

    private final JdbcClient jdbc;
    private final Clock relogio;

    public JdbcRepositorioDocumentoAdapter(JdbcClient jdbc, Clock relogio) {
        this.jdbc = jdbc;
        this.relogio = relogio;
    }

    @Override
    public Optional<DocumentoRegistrado> buscarPorHash(String hashConteudo) {
        return jdbc.sql("SELECT " + COLUNAS + " FROM conhecimento.documento WHERE hash_conteudo = :hash")
                .param("hash", hashConteudo)
                .query(JdbcRepositorioDocumentoAdapter::mapear)
                .optional();
    }

    @Override
    public List<DocumentoRegistrado> buscarAutorizadosPorTitulo(String titulo) {
        return jdbc.sql("SELECT " + COLUNAS + " FROM conhecimento.documento WHERE titulo = :titulo AND situacao = 'AUTORIZADO'")
                .param("titulo", titulo)
                .query(JdbcRepositorioDocumentoAdapter::mapear)
                .list();
    }

    @Override
    public List<DocumentoRegistrado> listarTodos() {
        return jdbc.sql("SELECT " + COLUNAS + " FROM conhecimento.documento ORDER BY cadastrado_em, titulo")
                .query(JdbcRepositorioDocumentoAdapter::mapear)
                .list();
    }

    @Override
    public UUID inserir(DocumentoParaIngestao d, String hashConteudo, String modeloEmbedding, String cadastradoPor) {
        UUID id = UUID.randomUUID();
        jdbc.sql("""
                INSERT INTO conhecimento.documento
                    (id, titulo, versao, vigente_desde, situacao, hash_conteudo, texto, quantidade_trechos,
                     modelo_embedding, arquivo_origem, cadastrado_por, cadastrado_em)
                VALUES (:id, :titulo, :versao, :vigente, 'PROCESSANDO', :hash, :texto, 0,
                        :modelo, :arquivo, :cadastradoPor, :cadastradoEm)
                """)
                .param("id", id)
                .param("titulo", d.titulo())
                .param("versao", d.versao())
                .param("vigente", d.vigenteDesde())
                .param("hash", hashConteudo)
                .param("texto", d.texto())
                .param("modelo", modeloEmbedding)
                .param("arquivo", d.arquivoOrigem())
                .param("cadastradoPor", cadastradoPor)
                .param("cadastradoEm", Timestamp.from(relogio.instant()))
                .update();
        return id;
    }

    /** {@code quantidadeTrechos} negativo mantém a contagem atual (usado ao desautorizar versão anterior). */
    @Override
    public void atualizarSituacao(UUID documentoId, SituacaoDocumento situacao, int quantidadeTrechos) {
        jdbc.sql("""
                UPDATE conhecimento.documento
                   SET situacao = :situacao,
                       quantidade_trechos = CASE WHEN :quantidade < 0 THEN quantidade_trechos ELSE :quantidade END
                 WHERE id = :id
                """)
                .param("situacao", situacao.name())
                .param("quantidade", quantidadeTrechos)
                .param("id", documentoId)
                .update();
    }

    @Override
    public void remover(UUID documentoId) {
        jdbc.sql("DELETE FROM conhecimento.documento WHERE id = :id").param("id", documentoId).update();
    }

    private static DocumentoRegistrado mapear(ResultSet rs, int linha) throws SQLException {
        return new DocumentoRegistrado(rs.getObject("id", UUID.class), rs.getString("titulo"), rs.getString("versao"),
                rs.getString("arquivo_origem"), rs.getString("hash_conteudo"),
                SituacaoDocumento.valueOf(rs.getString("situacao")));
    }
}
