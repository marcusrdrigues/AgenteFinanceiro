package br.com.financecorp.agentefinanceiro.aplicacao.modelo;

import java.util.Objects;

/** Fonte citada na resposta. Sempre derivada em código (da busca ou da consulta), nunca do texto do modelo. */
public record Fonte(TipoFonte tipo, String titulo, String versao, String secao) {

    public Fonte {
        Objects.requireNonNull(tipo, "tipo");
    }

    public static Fonte documento(String titulo, String versao, String secao) {
        return new Fonte(TipoFonte.DOCUMENTO, titulo, versao, secao);
    }
}
