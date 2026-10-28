package br.com.financecorp.agentefinanceiro.aplicacao.porta;

import br.com.financecorp.agentefinanceiro.aplicacao.modelo.ClassificacaoBruta;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.Turno;
import java.util.List;
import java.util.Optional;

/** Classifica e reescreve a pergunta (FR-039). {@code correcao} é o motivo da reprovação anterior. */
public interface ClassificadorPerguntaPort {

    ClassificacaoBruta classificar(String pergunta, List<Turno> historico, Optional<String> correcao);
}
