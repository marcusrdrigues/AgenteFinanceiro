package br.com.financecorp.agentefinanceiro.aplicacao.modelo;

/**
 * Uma mensagem do histórico da conversa. Criado no incremento 2 para que a porta do classificador já
 * nasça com o histórico; o histórico passa a ser preenchido no incremento 3.
 */
public record Turno(Papel papel, String texto) {

    public enum Papel { USUARIO, ASSISTENTE }
}
