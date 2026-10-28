package br.com.financecorp.agentefinanceiro.dominio.excecao;

/** O motivo descreve a regra violada, nunca o valor recebido. */
public final class PeriodoInvalidoException extends ExcecaoDominio {

    public PeriodoInvalidoException(String motivo) {
        super("periodo-invalido", "Período inválido: " + motivo + ".");
    }
}
