package br.com.financecorp.agentefinanceiro.infraestrutura.ia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.financecorp.agentefinanceiro.infraestrutura.config.PropriedadesIa;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

class CarregadorPromptsTest {

    private static CarregadorPrompts carregador(Map<String, String> versoes) {
        return new CarregadorPrompts(new DefaultResourceLoader(),
                new PropriedadesIa(2, 2, Duration.ofMillis(1), 2, Duration.ofSeconds(25), Duration.ofSeconds(30), versoes));
    }

    @Test
    void deveCarregarAVersaoConfigurada() {
        CarregadorPrompts c = carregador(Map.of("classificar", "v1", "responder-documental", "v1"));
        assertThat(c.system("classificar").getFilename()).isEqualTo("classificar.system.v1.st");
        assertThat(c.user("responder-documental").exists()).isTrue();
    }

    @Test
    void versaoInexistenteDeveFalharCitandoOArquivo() {
        assertThatThrownBy(() -> carregador(Map.of("classificar", "v9")).system("classificar"))
                .hasMessageContaining("prompts/classificar.system.v9.st");
    }

    @Test
    void promptSemVersaoConfiguradaDeveFalhar() {
        assertThatThrownBy(() -> carregador(Map.of()).system("classificar"))
                .hasMessageContaining("financeiro.ia.versao-prompts");
    }

    @Test
    void promptsNaoDevemTerChavesForaDasVariaveis() throws Exception {
        // O renderizador do Spring AI trata { } como variável: uma chave solta quebra a renderização.
        for (String arquivo : new String[] {"classificar.system.v1.st", "classificar.system.v2.st", "responder-documental.system.v1.st"}) {
            String texto = new DefaultResourceLoader().getResource("classpath:prompts/" + arquivo)
                    .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
            assertThat(texto).doesNotContain("{").doesNotContain("}");
        }
    }
}
