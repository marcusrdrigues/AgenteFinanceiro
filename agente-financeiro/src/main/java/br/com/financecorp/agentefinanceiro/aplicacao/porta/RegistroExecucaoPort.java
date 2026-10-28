package br.com.financecorp.agentefinanceiro.aplicacao.porta;

import br.com.financecorp.agentefinanceiro.aplicacao.modelo.RegistroExecucao;

/** Grava o registro de execução de uma requisição (FR-036 a FR-038). Falha ao gravar não derruba a resposta. */
public interface RegistroExecucaoPort {

    void registrar(RegistroExecucao registro);
}
