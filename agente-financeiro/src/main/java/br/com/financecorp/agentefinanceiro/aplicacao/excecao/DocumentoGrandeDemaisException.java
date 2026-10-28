package br.com.financecorp.agentefinanceiro.aplicacao.excecao;

/** O documento geraria mais trechos que o limite por documento (limite de consumo). */
public final class DocumentoGrandeDemaisException extends RuntimeException {

    public DocumentoGrandeDemaisException(int maximoTrechos) {
        super("Documento excede o limite de " + maximoTrechos + " trechos.");
    }
}
