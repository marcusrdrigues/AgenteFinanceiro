package br.com.financecorp.agentefinanceiro.dominio.excecao;

/** Provedor do modelo fora do ar, lento ou recusando a chamada. A mensagem nunca cita chave nem URL. */
public final class ModeloIndisponivelException extends ServicoIndisponivelException {

    public ModeloIndisponivelException() {
        super("modelo-indisponivel", "O assistente está temporariamente indisponível.", 30);
    }
}
