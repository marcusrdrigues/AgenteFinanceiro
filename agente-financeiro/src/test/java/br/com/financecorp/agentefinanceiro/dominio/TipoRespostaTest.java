package br.com.financecorp.agentefinanceiro.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class TipoRespostaTest {

    @Test
    void deveConterTodosOsTiposDePerguntaMaisNaoInterpretada() {
        assertThat(Arrays.stream(TipoResposta.values()).map(Enum::name))
                .containsAll(Arrays.stream(TipoPergunta.values()).map(Enum::name).toList())
                .contains("NAO_INTERPRETADA")
                .hasSize(TipoPergunta.values().length + 1);
    }

    @Test
    void deveConverterCadaTipoDePergunta() {
        for (TipoPergunta tipo : TipoPergunta.values()) {
            assertThat(TipoResposta.de(tipo).name()).isEqualTo(tipo.name());
        }
    }
}
