package br.com.financecorp.agentefinanceiro.aplicacao.modelo;

import java.util.List;
import java.util.Optional;

/** Bloco {@code <trechos>} pronto para o prompt e os trechos rotulados para validar a resposta. */
public record ContextoMontado(List<TrechoRotulado> trechos, String bloco) {

    public ContextoMontado {
        trechos = List.copyOf(trechos);
    }

    public static ContextoMontado vazio() {
        return new ContextoMontado(List.of(), "");
    }

    public boolean estaVazio() {
        return trechos.isEmpty();
    }

    public Optional<TrechoDocumento> porRotulo(String rotulo) {
        return trechos.stream().filter(t -> t.rotulo().equals(rotulo)).map(TrechoRotulado::trecho).findFirst();
    }
}
