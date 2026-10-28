package br.com.financecorp.agentefinanceiro.dominio.excecao;

public final class LimiteRequisicoesException extends ServicoIndisponivelException {

    public LimiteRequisicoesException(long segundosParaNovaTentativa) {
        super("limite-requisicoes", "Limite de requisições atingido. Tente novamente em instantes.",
                segundosParaNovaTentativa);
    }
}
