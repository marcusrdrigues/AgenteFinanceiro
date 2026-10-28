package br.com.financecorp.agentefinanceiro.infraestrutura.ia;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.financecorp.agentefinanceiro.infraestrutura.ia.TradutorExcecoesIa.Natureza;
import com.openai.core.http.Headers;
import com.openai.errors.InternalServerException;
import com.openai.errors.OpenAIIoException;
import com.openai.errors.RateLimitException;
import com.openai.errors.UnauthorizedException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.Test;

class TradutorExcecoesIaTest {

    private final TradutorExcecoesIa tradutor = new TradutorExcecoesIa();
    private final Headers semCabecalhos = Headers.builder().build();

    @Test
    void limiteServidorETimeoutSaoTransitorios() {
        assertThat(tradutor.classificar(RateLimitException.builder().headers(semCabecalhos).build()))
                .isEqualTo(Natureza.TRANSITORIA);
        assertThat(tradutor.classificar(InternalServerException.builder().statusCode(503).headers(semCabecalhos).build()))
                .isEqualTo(Natureza.TRANSITORIA);
        assertThat(tradutor.classificar(new OpenAIIoException("io"))).isEqualTo(Natureza.TRANSITORIA);
        assertThat(tradutor.classificar(new SocketTimeoutException())).isEqualTo(Natureza.TRANSITORIA);
        assertThat(tradutor.classificar(new ConnectException())).isEqualTo(Natureza.TRANSITORIA);
    }

    @Test
    void credencialRecusadaNaoETransitoria() {
        UnauthorizedException recusada = UnauthorizedException.builder().headers(semCabecalhos).build();
        assertThat(tradutor.classificar(recusada)).isEqualTo(Natureza.NAO_TRANSITORIA);
        assertThat(tradutor.ehFalhaDeCredencial(recusada)).isTrue();
    }

    @Test
    void causaEncadeadaDeveSerConsiderada() {
        RuntimeException embrulhada = new RuntimeException("falha", new OpenAIIoException("io"));
        assertThat(tradutor.classificar(embrulhada)).isEqualTo(Natureza.TRANSITORIA);
    }

    @Test
    void falhaDeConversaoNaoEDoProvedor() {
        assertThat(tradutor.classificar(new IllegalStateException("json"))).isEqualTo(Natureza.NAO_E_DO_PROVEDOR);
    }
}
