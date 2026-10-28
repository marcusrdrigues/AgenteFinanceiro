package br.com.financecorp.agentefinanceiro.infraestrutura.config;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.PropertyResolver;

/**
 * Para a subida antes de criar qualquer bean quando falta a chave ou o modelo do provedor. A
 * mensagem cita o nome da variável de ambiente, nunca o valor (constituição IX).
 */
public final class VerificadorConfiguracaoIa implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    private static final Map<String, String> OBRIGATORIAS = new LinkedHashMap<>();

    static {
        OBRIGATORIAS.put("spring.ai.openai.api-key", "OPENAI_API_KEY");
        OBRIGATORIAS.put("spring.ai.openai.chat.model", "OPENAI_CHAT_MODEL");
    }

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent evento) {
        verificar(evento.getEnvironment());
    }

    public static void verificar(PropertyResolver ambiente) {
        OBRIGATORIAS.forEach((propriedade, variavel) -> {
            String valor = ambiente.getProperty(propriedade);
            if (valor == null || valor.isBlank()) {
                throw new ConfiguracaoAusenteException(variavel, propriedade);
            }
        });
    }

    public static final class ConfiguracaoAusenteException extends IllegalStateException {

        ConfiguracaoAusenteException(String variavel, String propriedade) {
            super("Configuração obrigatória ausente: defina a variável de ambiente " + variavel
                    + " (propriedade " + propriedade + ").");
        }
    }
}
