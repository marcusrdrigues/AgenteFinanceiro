package br.com.financecorp.agentefinanceiro.dominio.excecao;

public final class FonteDadosIndisponivelException extends ServicoIndisponivelException {

    public FonteDadosIndisponivelException() {
        super("fonte-dados-indisponivel", "Os dados financeiros estão temporariamente indisponíveis.", 30);
    }
}
