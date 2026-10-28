package br.com.financecorp.agentefinanceiro.aplicacao.validacao;

import br.com.financecorp.agentefinanceiro.aplicacao.modelo.ClassificacaoBruta;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.ClassificacaoPergunta;
import br.com.financecorp.agentefinanceiro.dominio.Categoria;
import br.com.financecorp.agentefinanceiro.dominio.Periodo;
import br.com.financecorp.agentefinanceiro.dominio.TipoPergunta;
import br.com.financecorp.agentefinanceiro.dominio.excecao.PeriodoInvalidoException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;

/** Valida a saída do classificador contra a lista fechada e as regras de período (FR-039). */
public final class ValidadorClassificacao {

    /** Classificação válida ou o motivo da reprovação. */
    public sealed interface Resultado {
        record Valida(ClassificacaoPergunta classificacao) implements Resultado {
        }

        record Invalida(String motivo) implements Resultado {
        }
    }

    private final int tamanhoMaximoPergunta;

    public ValidadorClassificacao(int tamanhoMaximoPergunta) {
        this.tamanhoMaximoPergunta = tamanhoMaximoPergunta;
    }

    public Resultado validar(ClassificacaoBruta bruta) {
        if (bruta == null) {
            return invalida("A classificação veio vazia.");
        }
        TipoPergunta tipo;
        try {
            tipo = TipoPergunta.valueOf(texto(bruta.tipo()));
        } catch (IllegalArgumentException e) {
            return invalida("O tipo deve ser um destes: " + Arrays.toString(TipoPergunta.values()) + ".");
        }
        String autonoma = texto(bruta.perguntaAutonoma());
        if (autonoma.isEmpty() || autonoma.length() > tamanhoMaximoPergunta) {
            return invalida("perguntaAutonoma deve ser preenchida, com até " + tamanhoMaximoPergunta + " caracteres.");
        }
        String inicio = texto(bruta.inicio());
        String fim = texto(bruta.fim());
        Periodo periodo = null;
        if (!inicio.isEmpty() || !fim.isEmpty()) {
            if (inicio.isEmpty() || fim.isEmpty()) {
                return invalida("Informe inicio e fim juntos, ou deixe os dois vazios.");
            }
            try {
                periodo = Periodo.intervalo(LocalDate.parse(inicio), LocalDate.parse(fim));
            } catch (DateTimeParseException e) {
                return invalida("inicio e fim devem estar no formato AAAA-MM-DD.");
            } catch (PeriodoInvalidoException e) {
                return invalida(e.getMessage());
            }
        }
        Categoria categoria = null;
        String textoCategoria = texto(bruta.categoria());
        if (!textoCategoria.isEmpty()) {
            try {
                categoria = Categoria.valueOf(textoCategoria);
            } catch (IllegalArgumentException e) {
                return invalida("A categoria deve ser uma destas: " + Arrays.toString(Categoria.values()) + ".");
            }
        }
        return new Resultado.Valida(new ClassificacaoPergunta(tipo, autonoma, periodo, categoria));
    }

    private static Resultado invalida(String motivo) {
        return new Resultado.Invalida(motivo);
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor.strip();
    }
}
