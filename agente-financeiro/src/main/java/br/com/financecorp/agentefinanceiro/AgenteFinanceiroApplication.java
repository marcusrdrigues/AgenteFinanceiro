package br.com.financecorp.agentefinanceiro;

import br.com.financecorp.agentefinanceiro.infraestrutura.config.VerificadorConfiguracaoIa;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AgenteFinanceiroApplication {

    public static void main(String[] args) {
        criar().run(args);
    }

    /** Antes de qualquer bean: sem a chave ou o modelo, a subida para citando o nome da variável. */
    public static SpringApplication criar() {
        SpringApplication aplicacao = new SpringApplication(AgenteFinanceiroApplication.class);
        aplicacao.addListeners(new VerificadorConfiguracaoIa());
        return aplicacao;
    }
}
