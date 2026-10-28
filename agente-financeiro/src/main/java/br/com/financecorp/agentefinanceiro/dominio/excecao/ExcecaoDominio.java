package br.com.financecorp.agentefinanceiro.dominio.excecao;

/**
 * Base das exceções de domínio. O {@link #codigo()} identifica o problema na borda (ProblemDetail);
 * a mensagem é fixa, em português, e nunca ecoa o valor recebido do cliente.
 */
public abstract class ExcecaoDominio extends RuntimeException {

    private final String codigo;

    protected ExcecaoDominio(String codigo, String mensagem) {
        super(mensagem);
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }
}
