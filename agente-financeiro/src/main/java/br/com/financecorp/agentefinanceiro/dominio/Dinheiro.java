package br.com.financecorp.agentefinanceiro.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Objects;

/**
 * Valor monetário em reais. Único ponto do domínio que define escala e arredondamento de dinheiro
 * (constituição IV): escala 2, {@link RoundingMode#HALF_EVEN}.
 */
public record Dinheiro(BigDecimal valor) {

    private static final int ESCALA = 2;
    private static final RoundingMode ARREDONDAMENTO = RoundingMode.HALF_EVEN;

    public Dinheiro {
        Objects.requireNonNull(valor, "valor");
        // Escala normalizada aqui garante que equals e hashCode do record tratem 10.0 e 10.00 como iguais.
        valor = valor.setScale(ESCALA, ARREDONDAMENTO);
    }

    public static Dinheiro de(String valor) {
        return new Dinheiro(new BigDecimal(valor));
    }

    public static Dinheiro de(BigDecimal valor) {
        return new Dinheiro(valor);
    }

    public static Dinheiro zero() {
        return new Dinheiro(BigDecimal.ZERO);
    }

    public Dinheiro somar(Dinheiro outro) {
        return new Dinheiro(valor.add(outro.valor));
    }

    public Dinheiro subtrair(Dinheiro outro) {
        return new Dinheiro(valor.subtract(outro.valor));
    }

    public Dinheiro negativo() {
        return new Dinheiro(valor.negate());
    }

    public boolean ehZero() {
        return valor.signum() == 0;
    }

    public boolean ehNegativo() {
        return valor.signum() < 0;
    }

    /** Formato da resposta em texto (FR-005): {@code R$ 1.234,56} e {@code -R$ 12.500,00}. */
    public String formatarBrl() {
        String absoluto = formatoBrasileiro().format(valor.abs());
        return (ehNegativo() ? "-R$ " : "R$ ") + absoluto;
    }

    /** Formato dos campos estruturados da API e do A2A (FR-005): {@code "150000.00"}. */
    public String paraDecimalTexto() {
        return valor.toPlainString();
    }

    private static DecimalFormat formatoBrasileiro() {
        // Símbolos explícitos: o formato não pode depender do locale da máquina do aluno.
        DecimalFormatSymbols simbolos = new DecimalFormatSymbols();
        simbolos.setGroupingSeparator('.');
        simbolos.setDecimalSeparator(',');
        DecimalFormat formato = new DecimalFormat("#,##0.00", simbolos);
        formato.setRoundingMode(ARREDONDAMENTO);
        return formato;
    }
}
