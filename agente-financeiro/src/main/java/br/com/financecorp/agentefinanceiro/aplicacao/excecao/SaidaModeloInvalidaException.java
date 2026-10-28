package br.com.financecorp.agentefinanceiro.aplicacao.excecao;

/** A saída do modelo não converteu no schema. O motivo volta ao modelo na nova tentativa; não vai ao usuário. */
public final class SaidaModeloInvalidaException extends RuntimeException {

    public SaidaModeloInvalidaException(String motivo) {
        super(motivo);
    }
}
