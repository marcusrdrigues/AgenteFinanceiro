package br.com.financecorp.agentefinanceiro.aplicacao.porta;

import br.com.financecorp.agentefinanceiro.aplicacao.modelo.TrechoDocumento;
import java.util.List;

/**
 * Busca semântica nas políticas. Só trechos AUTORIZADO, do modelo de embedding atual e acima do
 * limiar, no máximo top-k; o filtro roda dentro da busca. Lista vazia é resultado válido.
 */
public interface BuscaDocumentoPort {

    List<TrechoDocumento> buscar(String pergunta);
}
