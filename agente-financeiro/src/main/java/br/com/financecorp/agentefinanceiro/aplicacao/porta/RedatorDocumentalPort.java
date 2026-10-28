package br.com.financecorp.agentefinanceiro.aplicacao.porta;

import br.com.financecorp.agentefinanceiro.aplicacao.modelo.ContextoMontado;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.RespostaDocumentalRedigida;
import java.util.Optional;

/** Redige a resposta só com os trechos do contexto; nunca é chamado com contexto vazio (FR-020). */
public interface RedatorDocumentalPort {

    RespostaDocumentalRedigida redigir(String pergunta, ContextoMontado contexto, Optional<String> correcao);
}
