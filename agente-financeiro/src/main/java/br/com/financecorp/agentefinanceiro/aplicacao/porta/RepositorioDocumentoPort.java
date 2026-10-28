package br.com.financecorp.agentefinanceiro.aplicacao.porta;

import br.com.financecorp.agentefinanceiro.aplicacao.modelo.DocumentoParaIngestao;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.DocumentoRegistrado;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.SituacaoDocumento;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioDocumentoPort {

    Optional<DocumentoRegistrado> buscarPorHash(String hashConteudo);

    /** Versões do mesmo título que ainda aparecem na busca, para desautorizar quando chega versão nova. */
    List<DocumentoRegistrado> buscarAutorizadosPorTitulo(String titulo);

    List<DocumentoRegistrado> listarTodos();

    UUID inserir(DocumentoParaIngestao documento, String hashConteudo, String modeloEmbedding, String cadastradoPor);

    void atualizarSituacao(UUID documentoId, SituacaoDocumento situacao, int quantidadeTrechos);

    void remover(UUID documentoId);
}
