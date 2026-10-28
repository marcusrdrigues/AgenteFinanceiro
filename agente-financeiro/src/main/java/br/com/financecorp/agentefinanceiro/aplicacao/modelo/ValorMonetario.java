package br.com.financecorp.agentefinanceiro.aplicacao.modelo;

import br.com.financecorp.agentefinanceiro.dominio.Dinheiro;

public record ValorMonetario(String rotulo, Dinheiro valor) {
}
