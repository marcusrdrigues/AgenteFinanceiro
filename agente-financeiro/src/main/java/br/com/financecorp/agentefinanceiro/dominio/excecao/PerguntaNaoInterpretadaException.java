package br.com.financecorp.agentefinanceiro.dominio.excecao;

public final class PerguntaNaoInterpretadaException extends ExcecaoDominio {

    public PerguntaNaoInterpretadaException() {
        super("pergunta-nao-interpretada", "Não foi possível interpretar a pergunta.");
    }
}
