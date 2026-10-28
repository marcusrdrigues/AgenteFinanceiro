package br.com.financecorp.agentefinanceiro.interfaces.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Gera o identificador de rastreamento de cada requisição (FR-036). É sempre gerado no servidor:
 * um valor vindo do cliente não é aceito, para não permitir injetar texto nos logs.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FiltroTraceId extends OncePerRequestFilter {

    public static final String CHAVE_MDC = "traceId";
    public static final String CABECALHO = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest requisicao, HttpServletResponse resposta, FilterChain cadeia)
            throws ServletException, IOException {
        String traceId = UUID.randomUUID().toString();
        MDC.put(CHAVE_MDC, traceId);
        resposta.setHeader(CABECALHO, traceId);
        try {
            cadeia.doFilter(requisicao, resposta);
        } finally {
            MDC.remove(CHAVE_MDC);
        }
    }
}
