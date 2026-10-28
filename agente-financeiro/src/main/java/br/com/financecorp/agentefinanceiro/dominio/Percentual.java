package br.com.financecorp.agentefinanceiro.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Objects;
import java.util.Optional;

/**
 * Razão entre dois valores (0.4688 = 46,88%). Único ponto do domínio que define escala e
 * arredondamento de razões (constituição IV): escala 4, {@link RoundingMode#HALF_EVEN}.
 */
public record Percentual(BigDecimal razao) {

    private static final int ESCALA = 4;
    private static final RoundingMode ARREDONDAMENTO = RoundingMode.HALF_EVEN;

    public Percentual {
        Objects.requireNonNull(razao, "razao");
        razao = razao.setScale(ESCALA, ARREDONDAMENTO);
    }

    /** Vazio quando {@code todo} é zero: a razão não existe e nunca há divisão por zero. */
    public static Optional<Percentual> razao(Dinheiro parte, Dinheiro todo) {
        if (todo.ehZero()) {
            return Optional.empty();
        }
        return Optional.of(new Percentual(parte.valor().divide(todo.valor(), ESCALA, ARREDONDAMENTO)));
    }

    public boolean maiorQue(BigDecimal limite) {
        return razao.compareTo(limite) > 0;
    }

    public boolean menorQue(BigDecimal limite) {
        return razao.compareTo(limite) < 0;
    }

    /** Uma casa decimal: {@code 46,9%}. */
    public String formatar() {
        DecimalFormatSymbols simbolos = new DecimalFormatSymbols();
        simbolos.setDecimalSeparator(',');
        DecimalFormat formato = new DecimalFormat("0.0", simbolos);
        formato.setRoundingMode(ARREDONDAMENTO);
        return formato.format(razao.movePointRight(2)) + "%";
    }
}
