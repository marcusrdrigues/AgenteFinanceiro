package br.com.financecorp.agentefinanceiro.aplicacao.servico;

import br.com.financecorp.agentefinanceiro.aplicacao.modelo.DocumentoRegistrado;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.SituacaoDocumento;
import br.com.financecorp.agentefinanceiro.aplicacao.porta.GravacaoTrechosPort;
import br.com.financecorp.agentefinanceiro.aplicacao.porta.RepositorioDocumentoPort;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * Publicação numa transação curta, depois que todos os embeddings já foram gravados: muda a
 * situação dos trechos e do documento juntos e desautoriza versões anteriores do mesmo título.
 * Não é final porque a transação é aplicada por proxy.
 */
public class PublicacaoDocumento {

    private final RepositorioDocumentoPort documentos;
    private final GravacaoTrechosPort trechos;

    public PublicacaoDocumento(RepositorioDocumentoPort documentos, GravacaoTrechosPort trechos) {
        this.documentos = documentos;
        this.trechos = trechos;
    }

    @Transactional
    public void publicar(UUID documentoId, String titulo, SituacaoDocumento situacao, int quantidadeTrechos) {
        if (situacao == SituacaoDocumento.AUTORIZADO) {
            for (DocumentoRegistrado anterior : documentos.buscarAutorizadosPorTitulo(titulo)) {
                if (!anterior.id().equals(documentoId)) {
                    trechos.atualizarSituacao(anterior.id(), SituacaoDocumento.NAO_AUTORIZADO);
                    documentos.atualizarSituacao(anterior.id(), SituacaoDocumento.NAO_AUTORIZADO, -1);
                }
            }
        }
        trechos.atualizarSituacao(documentoId, situacao);
        documentos.atualizarSituacao(documentoId, situacao, quantidadeTrechos);
    }
}
