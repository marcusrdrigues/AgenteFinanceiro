package br.com.financecorp.agentefinanceiro.infraestrutura.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentação OpenAPI gerada pelo springdoc (Swagger UI em /swagger-ui.html). O contrato de
 * referência continua sendo specs/001-assistente-financeiro/contracts/api-agente-financeiro.yaml.
 */
@Configuration
public class ConfiguracaoOpenApi {

    @Bean
    OpenAPI documentacaoApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("AgenteFinanceiro - API REST")
                        .version("1.0.0")
                        .description("Perguntas ao assistente financeiro da FinanceCorp (empresa fictícia). "
                                + "Sem autenticação: só ambiente local, não apto para produção.")
                        .license(new License().name("Uso didático")))
                .servers(List.of(new Server().url("http://localhost:8080").description("Local")));
    }
}
