package br.com.financecorp.agentefinanceiro.infraestrutura.ia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.financecorp.agentefinanceiro.dominio.excecao.ModeloIndisponivelException;
import br.com.financecorp.agentefinanceiro.infraestrutura.config.PropriedadesIa;
import com.openai.core.http.Headers;
import com.openai.errors.OpenAIIoException;
import com.openai.errors.RateLimitException;
import com.openai.errors.UnauthorizedException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ExecutorChamadaModeloTest {

    // Backoff de 1 ms: o teste confere a política de tentativas, não espera de verdade.
    private final ExecutorChamadaModelo executor = new ExecutorChamadaModelo(new TradutorExcecoesIa(),
            new PropriedadesIa(2, 2, Duration.ofMillis(1), 2.0, Duration.ofSeconds(25), Duration.ofSeconds(30), Map.of()));
    private final AtomicInteger chamadas = new AtomicInteger();

    @Test
    void erroTransitorioDeveSerRepetidoNoMaximoDuasVezes() {
        assertThatThrownBy(() -> executor.executar(() -> {
            chamadas.incrementAndGet();
            throw RateLimitException.builder().headers(Headers.builder().build()).build();
        })).isInstanceOf(ModeloIndisponivelException.class);
        assertThat(chamadas).hasValue(3);
    }

    @Test
    void erroTransitorioSeguidoDeSucessoDeveDevolverOResultado() {
        String resultado = executor.executar(() -> {
            if (chamadas.incrementAndGet() == 1) {
                throw new OpenAIIoException("timeout");
            }
            return "ok";
        });
        assertThat(resultado).isEqualTo("ok");
        assertThat(chamadas).hasValue(2);
    }

    @Test
    void credencialRecusadaNaoDeveSerRepetida() {
        assertThatThrownBy(() -> executor.executar(() -> {
            chamadas.incrementAndGet();
            throw UnauthorizedException.builder().headers(Headers.builder().build()).build();
        })).isInstanceOf(ModeloIndisponivelException.class)
                .hasMessageNotContaining("sk-")
                .hasMessageNotContaining("http");
        assertThat(chamadas).hasValue(1);
    }

    @Test
    void falhaQueNaoEDoProvedorDeveSubirComoVeio() {
        assertThatThrownBy(() -> executor.executar(() -> {
            chamadas.incrementAndGet();
            throw new IllegalArgumentException("conversão");
        })).isInstanceOf(IllegalArgumentException.class);
        assertThat(chamadas).hasValue(1);
    }
}
