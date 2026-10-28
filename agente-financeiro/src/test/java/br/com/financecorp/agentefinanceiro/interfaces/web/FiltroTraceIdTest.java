package br.com.financecorp.agentefinanceiro.interfaces.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class FiltroTraceIdTest {

    private final FiltroTraceId filtro = new FiltroTraceId();

    @Test
    void devePorOTraceIdNoMdcENoCabecalhoELimparDepois() throws Exception {
        AtomicReference<String> noMdcDuranteACadeia = new AtomicReference<>();
        MockHttpServletResponse resposta = new MockHttpServletResponse();

        filtro.doFilter(new MockHttpServletRequest(), resposta,
                (req, res) -> noMdcDuranteACadeia.set(MDC.get(FiltroTraceId.CHAVE_MDC)));

        String cabecalho = resposta.getHeader(FiltroTraceId.CABECALHO);
        assertThat(cabecalho).isNotBlank().isEqualTo(noMdcDuranteACadeia.get());
        assertThat(UUID.fromString(cabecalho)).isNotNull();
        assertThat(MDC.get(FiltroTraceId.CHAVE_MDC)).isNull();
    }

    @Test
    void naoDeveAceitarTraceIdEnviadoPeloCliente() throws Exception {
        MockHttpServletRequest requisicao = new MockHttpServletRequest();
        requisicao.addHeader(FiltroTraceId.CABECALHO, "valor\ninjetado");
        MockHttpServletResponse resposta = new MockHttpServletResponse();

        filtro.doFilter(requisicao, resposta, (req, res) -> { });

        assertThat(resposta.getHeader(FiltroTraceId.CABECALHO)).doesNotContain("injetado");
    }

    @Test
    void deveLimparOMdcMesmoQuandoACadeiaFalha() {
        try {
            filtro.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (req, res) -> {
                throw new IllegalStateException("falha");
            });
        } catch (Exception esperado) {
            // a exceção sobe; o que importa aqui é o MDC limpo
        }
        assertThat(MDC.get(FiltroTraceId.CHAVE_MDC)).isNull();
    }
}
