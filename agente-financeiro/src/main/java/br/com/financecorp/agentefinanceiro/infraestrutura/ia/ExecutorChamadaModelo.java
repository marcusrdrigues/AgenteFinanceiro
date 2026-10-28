package br.com.financecorp.agentefinanceiro.infraestrutura.ia;

import br.com.financecorp.agentefinanceiro.dominio.excecao.ModeloIndisponivelException;
import br.com.financecorp.agentefinanceiro.infraestrutura.config.PropriedadesIa;
import java.time.Duration;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.stereotype.Component;

/**
 * Único ponto de novas tentativas em chamadas ao provedor (research D14): só erro transitório, com
 * backoff exponencial e variação aleatória, até {@code tentativasTransitorias}. O cliente OpenAI fica
 * com {@code max-retries: 0} para as tentativas não se multiplicarem. Falha do provedor vira
 * {@link ModeloIndisponivelException}; qualquer outra exceção sobe como veio.
 */
@Component
public final class ExecutorChamadaModelo {

    private static final Logger LOG = LoggerFactory.getLogger(ExecutorChamadaModelo.class);

    private final TradutorExcecoesIa tradutor;
    private final RetryTemplate retry;

    public ExecutorChamadaModelo(TradutorExcecoesIa tradutor, PropriedadesIa propriedades) {
        this.tradutor = tradutor;
        Duration atraso = propriedades.backoffInicial();
        this.retry = new RetryTemplate(RetryPolicy.builder()
                .maxRetries(propriedades.tentativasTransitorias())
                .delay(atraso)
                .multiplier(propriedades.multiplicadorBackoff())
                .jitter(atraso.dividedBy(2))
                .predicate(tradutor::ehTransitoria)
                .build());
    }

    public <T> T executar(Supplier<T> chamada) {
        try {
            return retry.execute(chamada::get);
        } catch (RetryException esgotado) {
            Throwable causa = esgotado.getCause() != null ? esgotado.getCause() : esgotado;
            if (tradutor.classificar(causa) != TradutorExcecoesIa.Natureza.NAO_E_DO_PROVEDOR) {
                if (tradutor.ehFalhaDeCredencial(causa)) {
                    // Alerta de configuração, sem a chave nem a URL.
                    LOG.error("Provedor do modelo recusou a credencial; confira OPENAI_API_KEY.");
                } else {
                    LOG.warn("Provedor do modelo indisponível: {}", causa.getClass().getSimpleName());
                }
                throw new ModeloIndisponivelException();
            }
            if (causa instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException("Falha inesperada na chamada ao modelo", causa);
        }
    }
}
