package br.com.financecorp.agentefinanceiro.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PercentualTest {

    @Test
    void deveNormalizarParaQuatroCasasComHalfEven() {
        assertThat(new Percentual(new BigDecimal("0.46875")).razao()).isEqualByComparingTo("0.4688");
        assertThat(new Percentual(new BigDecimal("0.46885")).razao()).isEqualByComparingTo("0.4688");
        assertThat(new Percentual(new BigDecimal("0.5")).razao().scale()).isEqualTo(4);
    }

    @Test
    void deveCalcularRazaoEntreValores() {
        assertThat(Percentual.razao(Dinheiro.de("150000.00"), Dinheiro.de("320000.00")))
                .contains(new Percentual(new BigDecimal("0.4688")));
    }

    @Test
    void naoDeveDividirPorZero() {
        assertThat(Percentual.razao(Dinheiro.de("10.00"), Dinheiro.zero())).isEmpty();
    }

    @Test
    void deveFormatarComUmaCasa() {
        assertThat(new Percentual(new BigDecimal("0.4688")).formatar()).isEqualTo("46,9%");
        assertThat(new Percentual(new BigDecimal("0.05")).formatar()).isEqualTo("5,0%");
    }

    @Test
    void naoDeveSerMaiorQueLimiteIgual() {
        assertThat(new Percentual(new BigDecimal("0.9000")).maiorQue(new BigDecimal("0.90"))).isFalse();
        assertThat(new Percentual(new BigDecimal("0.9001")).maiorQue(new BigDecimal("0.90"))).isTrue();
    }
}
