package br.com.financecorp.agentefinanceiro.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class DinheiroTest {

    @Test
    void deveArredondarParaDuasCasasComHalfEven() {
        assertThat(Dinheiro.de("10.005").valor()).isEqualByComparingTo("10.00");
        assertThat(Dinheiro.de("10.015").valor()).isEqualByComparingTo("10.02");
        assertThat(Dinheiro.de("10.005").valor().scale()).isEqualTo(2);
    }

    @Test
    void deveRecusarValorNulo() {
        assertThatThrownBy(() -> new Dinheiro(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void deveConsiderarIguaisValoresComEscalasDiferentes() {
        assertThat(new Dinheiro(new BigDecimal("10.0"))).isEqualTo(new Dinheiro(new BigDecimal("10.00")));
        assertThat(new Dinheiro(new BigDecimal("10.0")).hashCode())
                .isEqualTo(new Dinheiro(new BigDecimal("10.00")).hashCode());
    }

    @Test
    void deveSomarSubtrairENegar() {
        Dinheiro cem = Dinheiro.de("100.00");
        assertThat(cem.somar(Dinheiro.de("0.50"))).isEqualTo(Dinheiro.de("100.50"));
        assertThat(cem.subtrair(Dinheiro.de("150.00"))).isEqualTo(Dinheiro.de("-50.00"));
        assertThat(cem.negativo()).isEqualTo(Dinheiro.de("-100.00"));
    }

    @Test
    void deveIndicarZeroENegativo() {
        assertThat(Dinheiro.zero().ehZero()).isTrue();
        assertThat(Dinheiro.de("-0.01").ehNegativo()).isTrue();
        assertThat(Dinheiro.de("0.00").ehNegativo()).isFalse();
    }

    @Test
    void deveFormatarEmReais() {
        assertThat(Dinheiro.de("1234.56").formatarBrl()).isEqualTo("R$ 1.234,56");
        assertThat(Dinheiro.de("-12500").formatarBrl()).isEqualTo("-R$ 12.500,00");
        assertThat(Dinheiro.zero().formatarBrl()).isEqualTo("R$ 0,00");
        assertThat(Dinheiro.de("1500000.5").formatarBrl()).isEqualTo("R$ 1.500.000,50");
    }

    @Test
    void deveConverterParaTextoDecimal() {
        assertThat(Dinheiro.de("150000").paraDecimalTexto()).isEqualTo("150000.00");
        assertThat(Dinheiro.de("-12500").paraDecimalTexto()).isEqualTo("-12500.00");
    }
}
