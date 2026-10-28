package br.com.financecorp.agentefinanceiro.infraestrutura.ia;

import br.com.financecorp.agentefinanceiro.aplicacao.modelo.Consumo;
import br.com.financecorp.agentefinanceiro.aplicacao.porta.ConsumoModeloPort;
import org.springframework.stereotype.Component;

/**
 * Acumula por thread: cada requisição HTTP roda inteira numa thread (fluxo síncrono, sem chamadas
 * paralelas ao modelo). Se um dia o fluxo passar a usar threads virtuais por etapa ou paralelismo, a
 * propagação do contexto precisa ser revista.
 */
@Component
public final class ConsumoModeloPorThread implements ConsumoModeloPort {

    private final ThreadLocal<long[]> acumulado = new ThreadLocal<>();

    @Override
    public void somar(int tokensEntrada, int tokensSaida) {
        long[] atual = acumulado.get();
        if (atual == null) {
            atual = new long[2];
            acumulado.set(atual);
        }
        atual[0] += tokensEntrada;
        atual[1] += tokensSaida;
    }

    @Override
    public Consumo coletarEZerar() {
        long[] atual = acumulado.get();
        acumulado.remove();
        return atual == null ? Consumo.NENHUM : new Consumo(Math.toIntExact(atual[0]), Math.toIntExact(atual[1]));
    }
}
