package br.com.financecorp.agentefinanceiro.dominio.excecao;

/** Dependência fora do ar ou limite atingido: a borda responde com Retry-After. */
public abstract class ServicoIndisponivelException extends ExcecaoDominio {

    private final long segundosParaNovaTentativa;

    protected ServicoIndisponivelException(String codigo, String mensagem, long segundosParaNovaTentativa) {
        super(codigo, mensagem);
        this.segundosParaNovaTentativa = segundosParaNovaTentativa;
    }

    public long segundosParaNovaTentativa() {
        return segundosParaNovaTentativa;
    }
}
