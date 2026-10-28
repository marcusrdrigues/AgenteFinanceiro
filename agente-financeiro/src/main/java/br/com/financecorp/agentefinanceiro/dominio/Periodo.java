package br.com.financecorp.agentefinanceiro.dominio;

import br.com.financecorp.agentefinanceiro.dominio.excecao.PeriodoInvalidoException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

/** Intervalo de datas com as duas pontas incluídas (FR-016). */
public record Periodo(LocalDate inicio, LocalDate fim) {

    /** Limite de consumo: uma consulta cobre no máximo um ano, inclusive bissexto. */
    public static final int MAXIMO_DIAS = 366;

    public Periodo {
        if (inicio == null || fim == null) {
            throw new PeriodoInvalidoException("início e fim são obrigatórios");
        }
        if (fim.isBefore(inicio)) {
            throw new PeriodoInvalidoException("a data final é anterior à inicial");
        }
        if (ChronoUnit.DAYS.between(inicio, fim) + 1 > MAXIMO_DIAS) {
            throw new PeriodoInvalidoException("o período passa de " + MAXIMO_DIAS + " dias");
        }
    }

    public static Periodo intervalo(LocalDate inicio, LocalDate fim) {
        return new Periodo(inicio, fim);
    }

    public static Periodo mes(YearMonth mes) {
        return new Periodo(mes.atDay(1), mes.atEndOfMonth());
    }

    public static Periodo trimestre(int ano, int numero) {
        if (numero < 1 || numero > 4) {
            throw new PeriodoInvalidoException("trimestre deve estar entre 1 e 4");
        }
        YearMonth primeiroMes = YearMonth.of(ano, (numero - 1) * 3 + 1);
        return new Periodo(primeiroMes.atDay(1), primeiroMes.plusMonths(2).atEndOfMonth());
    }

    public static Periodo ano(int ano) {
        return new Periodo(LocalDate.of(ano, 1, 1), LocalDate.of(ano, 12, 31));
    }

    public long quantidadeDias() {
        return ChronoUnit.DAYS.between(inicio, fim) + 1;
    }

    public boolean contem(LocalDate data) {
        return !data.isBefore(inicio) && !data.isAfter(fim);
    }

    /**
     * Período imediatamente anterior, usado na comparação de receita (FR-022). Quando o período
     * cobre meses inteiros (mês, trimestre, ano), volta o mesmo número de meses do calendário —
     * o 1º tri/2025 compara com o 4º tri/2024, não com "os 90 dias anteriores". Caso contrário,
     * volta a mesma quantidade de dias.
     */
    public Periodo anteriorDeMesmaDuracao() {
        if (cobreMesesInteiros()) {
            long meses = ChronoUnit.MONTHS.between(YearMonth.from(inicio), YearMonth.from(fim)) + 1;
            YearMonth primeiro = YearMonth.from(inicio).minusMonths(meses);
            return new Periodo(primeiro.atDay(1), YearMonth.from(inicio).minusMonths(1).atEndOfMonth());
        }
        long dias = quantidadeDias();
        return new Periodo(inicio.minusDays(dias), inicio.minusDays(1));
    }

    private boolean cobreMesesInteiros() {
        return inicio.getDayOfMonth() == 1 && fim.equals(YearMonth.from(fim).atEndOfMonth());
    }
}
