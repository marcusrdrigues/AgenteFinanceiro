package br.com.financecorp.agentefinanceiro.infraestrutura.ia;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.financecorp.agentefinanceiro.aplicacao.modelo.Consumo;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ConsumoModeloPorThreadTest {

    private final ConsumoModeloPorThread consumo = new ConsumoModeloPorThread();

    @Test
    void semChamadasDeveDevolverNenhum() {
        assertThat(consumo.coletarEZerar()).isEqualTo(Consumo.NENHUM);
    }

    @Test
    void deveSomarVariasChamadasEZerarAoColetar() {
        consumo.somar(500, 40);
        consumo.somar(1200, 90);

        assertThat(consumo.coletarEZerar()).isEqualTo(new Consumo(1700, 130));
        assertThat(consumo.coletarEZerar()).isEqualTo(Consumo.NENHUM);
    }

    @Test
    void requisicoesEmThreadsDiferentesNaoSeMisturam() throws Exception {
        consumo.somar(10, 1);
        AtomicReference<Consumo> daOutraThread = new AtomicReference<>();
        Thread outra = new Thread(() -> {
            consumo.somar(7, 3);
            daOutraThread.set(consumo.coletarEZerar());
        });
        outra.start();
        outra.join();

        assertThat(daOutraThread.get()).isEqualTo(new Consumo(7, 3));
        assertThat(consumo.coletarEZerar()).isEqualTo(new Consumo(10, 1));
    }
}
