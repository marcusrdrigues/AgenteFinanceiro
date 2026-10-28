package br.com.financecorp.agentefinanceiro.aplicacao.validacao;

import java.util.Optional;

/** Aprovado ou reprovado com o motivo, que volta ao modelo na nova tentativa. */
public record ResultadoValidacao(boolean aprovado, String motivo) {

    public static ResultadoValidacao aprovar() {
        return new ResultadoValidacao(true, null);
    }

    public static ResultadoValidacao reprovar(String motivo) {
        return new ResultadoValidacao(false, motivo);
    }

    public Optional<String> motivoReprovacao() {
        return aprovado ? Optional.empty() : Optional.of(motivo);
    }
}
