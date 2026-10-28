package br.com.financecorp.agentefinanceiro.dominio.excecao;

public final class EspecialistaIndisponivelException extends ExcecaoDominio {

    public EspecialistaIndisponivelException() {
        super("especialista-indisponivel", "A avaliação especializada não está disponível no momento.");
    }
}
