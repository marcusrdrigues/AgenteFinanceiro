package br.com.financecorp.agentefinanceiro.dominio.excecao;

public final class ConversaIdInvalidoException extends ExcecaoDominio {

    public ConversaIdInvalidoException() {
        super("conversa-id-invalido", "O identificador de conversa deve ser um UUID versão 4.");
    }
}
