package br.com.financecorp.agentefinanceiro.dominio.excecao;

public final class PerguntaInvalidaException extends ExcecaoDominio {

    public enum Motivo { VAZIA, MUITO_LONGA }

    private final Motivo motivo;

    public PerguntaInvalidaException(Motivo motivo) {
        super(motivo == Motivo.VAZIA ? "pergunta-vazia" : "pergunta-muito-longa",
                motivo == Motivo.VAZIA ? "A pergunta não pode ser vazia." : "A pergunta passa do tamanho máximo.");
        this.motivo = motivo;
    }

    public Motivo motivo() {
        return motivo;
    }
}
