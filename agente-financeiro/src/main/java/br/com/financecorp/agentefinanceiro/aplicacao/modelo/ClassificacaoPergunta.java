package br.com.financecorp.agentefinanceiro.aplicacao.modelo;

import br.com.financecorp.agentefinanceiro.dominio.Categoria;
import br.com.financecorp.agentefinanceiro.dominio.Periodo;
import br.com.financecorp.agentefinanceiro.dominio.TipoPergunta;
import java.util.Objects;

/** Classificação validada (FR-039). {@code periodo} e {@code categoria} podem ser nulos. */
public record ClassificacaoPergunta(TipoPergunta tipo, String perguntaAutonoma, Periodo periodo, Categoria categoria) {

    public ClassificacaoPergunta {
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(perguntaAutonoma, "perguntaAutonoma");
    }
}
