package br.com.financecorp.agentefinanceiro.aplicacao.porta;

import br.com.financecorp.agentefinanceiro.aplicacao.modelo.SituacaoDocumento;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.TrechoParaGravar;
import java.util.List;
import java.util.UUID;

/** Gravação na base vetorial. O embedding é gerado aqui, fora de qualquer transação. */
public interface GravacaoTrechosPort {

    void gravar(List<TrechoParaGravar> trechos);

    void removerPorDocumento(UUID documentoId);

    void atualizarSituacao(UUID documentoId, SituacaoDocumento situacao);

    /** Identificador com versão do modelo de embedding em uso (constituição VI). */
    String modeloEmbedding();
}
