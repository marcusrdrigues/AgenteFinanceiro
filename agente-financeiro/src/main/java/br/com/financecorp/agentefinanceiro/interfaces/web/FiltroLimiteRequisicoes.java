package br.com.financecorp.agentefinanceiro.interfaces.web;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Limite de requisições (FR-012): por conversa e no total, com Resilience4j RateLimiter. Fica na
 * borda e responde 429 em ProblemDetail com Retry-After antes de qualquer chamada ao modelo.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class FiltroLimiteRequisicoes extends OncePerRequestFilter {

    private static final Pattern CAMINHO = Pattern.compile("^/api/v1/conversas/([^/]+)/perguntas/?$");
    private static final Pattern UUID_V4 = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$");
    /** Teto de limitadores por conversa em memória; o mais antigo sai primeiro. */
    private static final int MAXIMO_CONVERSAS = 10_000;
    private static final Duration JANELA = Duration.ofMinutes(1);

    private final RateLimiter total;
    private final RateLimiterConfig porConversa;
    private final Map<String, RateLimiter> limitadores = Collections.synchronizedMap(
            new LinkedHashMap<>(256, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, RateLimiter> mais) {
                    return size() > MAXIMO_CONVERSAS;
                }
            });

    public FiltroLimiteRequisicoes(
            @Value("${financeiro.limites.requisicoes-por-minuto-total}") int porMinutoTotal,
            @Value("${financeiro.limites.requisicoes-por-minuto-por-conversa}") int porMinutoPorConversa) {
        this.total = RateLimiter.of("perguntas-total", config(porMinutoTotal));
        this.porConversa = config(porMinutoPorConversa);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest requisicao) {
        return !"POST".equals(requisicao.getMethod()) || !CAMINHO.matcher(requisicao.getRequestURI()).matches();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requisicao, HttpServletResponse resposta, FilterChain cadeia)
            throws ServletException, IOException {
        Matcher m = CAMINHO.matcher(requisicao.getRequestURI());
        String conversa = m.matches() ? m.group(1) : "";
        // Identificador fora do formato não ganha limitador próprio: o caso de uso devolve 400.
        boolean conversaPermitida = !UUID_V4.matcher(conversa).matches()
                || limitadores.computeIfAbsent(conversa.toLowerCase(), c -> RateLimiter.of("conversa", porConversa))
                        .acquirePermission();
        if (!conversaPermitida || !total.acquirePermission()) {
            recusar(resposta);
            return;
        }
        cadeia.doFilter(requisicao, resposta);
    }

    private static void recusar(HttpServletResponse resposta) throws IOException {
        String traceId = MDC.get(FiltroTraceId.CHAVE_MDC);
        resposta.setStatus(429);
        resposta.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(JANELA.toSeconds()));
        resposta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        resposta.setCharacterEncoding("UTF-8");
        resposta.getWriter().write("{\"type\":\"" + ErroHandler.BASE_TIPO + "limite-requisicoes\","
                + "\"title\":\"Limite de requisições atingido. Tente novamente em instantes.\",\"status\":429,"
                + "\"instance\":\"urn:financecorp:requisicao:" + traceId + "\",\"traceId\":\"" + traceId + "\"}");
    }

    private static RateLimiterConfig config(int porMinuto) {
        return RateLimiterConfig.custom()
                .limitForPeriod(porMinuto)
                .limitRefreshPeriod(JANELA)
                .timeoutDuration(Duration.ZERO)
                .build();
    }
}
