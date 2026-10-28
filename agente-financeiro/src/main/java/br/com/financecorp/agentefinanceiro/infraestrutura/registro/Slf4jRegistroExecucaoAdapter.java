package br.com.financecorp.agentefinanceiro.infraestrutura.registro;

import br.com.financecorp.agentefinanceiro.aplicacao.modelo.RegistroExecucao;
import br.com.financecorp.agentefinanceiro.aplicacao.porta.RegistroExecucaoPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Uma linha JSON por requisição no logger {@code execucao}, que o logback-spring.xml manda para
 * arquivo próprio com rotação diária e 90 dias de retenção (research D13).
 */
@Component
public final class Slf4jRegistroExecucaoAdapter implements RegistroExecucaoPort {

    static final String LOGGER = "execucao";
    private static final Logger EXECUCAO = LoggerFactory.getLogger(LOGGER);
    private static final Logger LOG = LoggerFactory.getLogger(Slf4jRegistroExecucaoAdapter.class);

    private final JsonMapper json;

    public Slf4jRegistroExecucaoAdapter(JsonMapper json) {
        this.json = json;
    }

    @Override
    public void registrar(RegistroExecucao registro) {
        try {
            EXECUCAO.info(json.writeValueAsString(registro));
        } catch (RuntimeException falha) {
            // Registrar não pode derrubar a resposta ao usuário; o aviso não leva o conteúdo.
            LOG.warn("Falha ao gravar registro de execução do traceId {}", registro.traceId());
        }
    }
}
