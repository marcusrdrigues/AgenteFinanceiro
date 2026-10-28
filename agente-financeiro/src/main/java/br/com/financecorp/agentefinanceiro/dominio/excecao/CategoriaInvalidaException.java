package br.com.financecorp.agentefinanceiro.dominio.excecao;

public final class CategoriaInvalidaException extends ExcecaoDominio {

    public CategoriaInvalidaException() {
        super("categoria-invalida", "A categoria informada não existe.");
    }
}
