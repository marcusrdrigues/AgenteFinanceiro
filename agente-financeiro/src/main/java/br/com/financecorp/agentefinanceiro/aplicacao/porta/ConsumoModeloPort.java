package br.com.financecorp.agentefinanceiro.aplicacao.porta;

import br.com.financecorp.agentefinanceiro.aplicacao.modelo.Consumo;

/**
 * Soma o consumo de tokens das chamadas ao modelo feitas durante uma requisição. Os adaptadores de
 * IA somam; o caso de uso coleta uma vez, no fim, para o registro de execução (FR-037). Zera ao coletar.
 */
public interface ConsumoModeloPort {

    void somar(int tokensEntrada, int tokensSaida);

    /** Devolve o acumulado e zera. {@link Consumo#NENHUM} se nada foi somado. */
    Consumo coletarEZerar();
}
