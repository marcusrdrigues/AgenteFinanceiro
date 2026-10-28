package br.com.financecorp.agentefinanceiro.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.financecorp.agentefinanceiro.dominio.excecao.PeriodoInvalidoException;
import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

class PeriodoTest {

    @Test
    void deveAceitarInicioIgualAoFimComoUmDia() {
        Periodo dia = Periodo.intervalo(LocalDate.of(2025, 3, 1), LocalDate.of(2025, 3, 1));
        assertThat(dia.contem(LocalDate.of(2025, 3, 1))).isTrue();
        assertThat(dia.quantidadeDias()).isEqualTo(1);
    }

    @Test
    void deveIncluirAsDuasPontas() {
        Periodo marco = Periodo.mes(YearMonth.of(2025, 3));
        assertThat(marco.contem(LocalDate.of(2025, 3, 1))).isTrue();
        assertThat(marco.contem(LocalDate.of(2025, 3, 31))).isTrue();
        assertThat(marco.contem(LocalDate.of(2025, 4, 1))).isFalse();
    }

    @Test
    void deveRecusarFimAntesDoInicio() {
        assertThatThrownBy(() -> Periodo.intervalo(LocalDate.of(2025, 3, 2), LocalDate.of(2025, 3, 1)))
                .isInstanceOf(PeriodoInvalidoException.class);
    }

    @Test
    void deveRecusarDatasNulas() {
        assertThatThrownBy(() -> Periodo.intervalo(null, LocalDate.of(2025, 3, 1)))
                .isInstanceOf(PeriodoInvalidoException.class);
    }

    @Test
    void deveRecusarMaisDe366Dias() {
        assertThatThrownBy(() -> Periodo.intervalo(LocalDate.of(2024, 1, 1), LocalDate.of(2025, 1, 1)))
                .isInstanceOf(PeriodoInvalidoException.class);
    }

    @Test
    void deveAceitarAnoBissextoCom366Dias() {
        Periodo ano = Periodo.ano(2024);
        assertThat(ano.quantidadeDias()).isEqualTo(366);
        assertThat(ano.inicio()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(ano.fim()).isEqualTo(LocalDate.of(2024, 12, 31));
    }

    @Test
    void deveCriarMes() {
        Periodo fevereiro = Periodo.mes(YearMonth.of(2025, 2));
        assertThat(fevereiro.inicio()).isEqualTo(LocalDate.of(2025, 2, 1));
        assertThat(fevereiro.fim()).isEqualTo(LocalDate.of(2025, 2, 28));
    }

    @Test
    void deveCriarTrimestre() {
        Periodo primeiro = Periodo.trimestre(2025, 1);
        assertThat(primeiro.inicio()).isEqualTo(LocalDate.of(2025, 1, 1));
        assertThat(primeiro.fim()).isEqualTo(LocalDate.of(2025, 3, 31));
    }

    @Test
    void deveRecusarTrimestreInexistente() {
        assertThatThrownBy(() -> Periodo.trimestre(2025, 5)).isInstanceOf(PeriodoInvalidoException.class);
    }

    @Test
    void deveDevolverTrimestreAnteriorParaTrimestre() {
        Periodo anterior = Periodo.trimestre(2025, 1).anteriorDeMesmaDuracao();
        assertThat(anterior).isEqualTo(Periodo.trimestre(2024, 4));
    }

    @Test
    void deveDevolverMesAnteriorParaMes() {
        assertThat(Periodo.mes(YearMonth.of(2025, 3)).anteriorDeMesmaDuracao())
                .isEqualTo(Periodo.mes(YearMonth.of(2025, 2)));
    }

    @Test
    void deveDevolverIntervaloDeMesmosDiasParaPeriodoLivre() {
        Periodo dezDias = Periodo.intervalo(LocalDate.of(2025, 3, 11), LocalDate.of(2025, 3, 20));
        assertThat(dezDias.anteriorDeMesmaDuracao())
                .isEqualTo(Periodo.intervalo(LocalDate.of(2025, 3, 1), LocalDate.of(2025, 3, 10)));
    }
}
