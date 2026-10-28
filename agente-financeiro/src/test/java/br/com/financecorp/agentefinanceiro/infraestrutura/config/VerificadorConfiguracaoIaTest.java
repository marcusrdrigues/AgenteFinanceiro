package br.com.financecorp.agentefinanceiro.infraestrutura.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.financecorp.agentefinanceiro.AgenteFinanceiroApplication;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class VerificadorConfiguracaoIaTest {

    @Test
    void deveFalharCitandoOpenaiApiKeyQuandoAChaveFalta() {
        MockEnvironment ambiente = new MockEnvironment().withProperty("spring.ai.openai.chat.model", "modelo");
        assertThatThrownBy(() -> VerificadorConfiguracaoIa.verificar(ambiente))
                .hasMessageContaining("OPENAI_API_KEY");
    }

    @Test
    void deveFalharCitandoOpenaiChatModelQuandoOModeloEstaEmBranco() {
        MockEnvironment ambiente = new MockEnvironment()
                .withProperty("spring.ai.openai.api-key", "sk-valor-que-nao-pode-aparecer")
                .withProperty("spring.ai.openai.chat.model", "  ");
        assertThatThrownBy(() -> VerificadorConfiguracaoIa.verificar(ambiente))
                .hasMessageContaining("OPENAI_CHAT_MODEL")
                .hasMessageNotContaining("sk-valor-que-nao-pode-aparecer");
    }

    @Test
    void devePassarComChaveEModelo() {
        MockEnvironment ambiente = new MockEnvironment()
                .withProperty("spring.ai.openai.api-key", "chave")
                .withProperty("spring.ai.openai.chat.model", "modelo");
        assertThatCode(() -> VerificadorConfiguracaoIa.verificar(ambiente)).doesNotThrowAnyException();
    }

    @Test
    void subidaRealSemChaveDevePararAntesDosBeans() {
        // Passa a chave vazia explicitamente: a máquina pode ter OPENAI_API_KEY definida.
        assertThatThrownBy(() -> AgenteFinanceiroApplication.criar()
                .run("--spring.ai.openai.api-key=", "--spring.ai.openai.chat.model=modelo",
                        "--spring.main.web-application-type=none"))
                .hasMessageContaining("OPENAI_API_KEY");
    }
}
