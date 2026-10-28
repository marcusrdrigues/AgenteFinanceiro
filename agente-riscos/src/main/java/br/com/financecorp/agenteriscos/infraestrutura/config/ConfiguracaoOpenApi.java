package br.com.financecorp.agenteriscos.infraestrutura.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentação OpenAPI do agente-riscos (Swagger UI em /swagger-ui.html). O contrato entre agentes
 * é o A2A (Agent Card em /.well-known/agent-card.json, a partir do incremento 6), não REST: esta
 * página documenta só os endpoints HTTP do serviço e aponta para o contrato A2A.
 */
@Configuration
public class ConfiguracaoOpenApi {

    @Bean
    OpenAPI documentacaoApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("agente-riscos")
                        .version("1.0.0")
                        .description("Agente especialista em risco financeiro por regras fixas, sem modelo de "
                                + "linguagem. A comunicação com o agente-financeiro é pelo protocolo A2A; o contrato "
                                + "está em specs/001-assistente-financeiro/contracts/a2a-avaliacao-risco.md.")
                        .license(new License().name("Uso didático")))
                .servers(List.of(new Server().url("http://localhost:8081").description("Local")));
    }
}
