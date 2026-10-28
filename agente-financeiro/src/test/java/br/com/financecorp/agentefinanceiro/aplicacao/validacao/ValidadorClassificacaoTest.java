package br.com.financecorp.agentefinanceiro.aplicacao.validacao;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.financecorp.agentefinanceiro.aplicacao.modelo.ClassificacaoBruta;
import br.com.financecorp.agentefinanceiro.aplicacao.validacao.ValidadorClassificacao.Resultado;
import br.com.financecorp.agentefinanceiro.dominio.Categoria;
import br.com.financecorp.agentefinanceiro.dominio.Periodo;
import br.com.financecorp.agentefinanceiro.dominio.TipoPergunta;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

class ValidadorClassificacaoTest {

    private final ValidadorClassificacao validador = new ValidadorClassificacao(1000);

    @Test
    void deveAceitarClassificacaoCompleta() {
        Resultado r = validador.validar(new ClassificacaoBruta(
                "NUMERICA", "Qual a receita de março de 2025?", "2025-03-01", "2025-03-31", "VENDAS"));
        assertThat(r).isInstanceOfSatisfying(Resultado.Valida.class, v -> {
            assertThat(v.classificacao().tipo()).isEqualTo(TipoPergunta.NUMERICA);
            assertThat(v.classificacao().periodo()).isEqualTo(Periodo.mes(YearMonth.of(2025, 3)));
            assertThat(v.classificacao().categoria()).isEqualTo(Categoria.VENDAS);
        });
    }

    @Test
    void camposVaziosDevemVirarNulos() {
        Resultado r = validador.validar(new ClassificacaoBruta("DOCUMENTAL", "Qual o limite?", "", " ", ""));
        assertThat(r).isInstanceOfSatisfying(Resultado.Valida.class, v -> {
            assertThat(v.classificacao().periodo()).isNull();
            assertThat(v.classificacao().categoria()).isNull();
        });
    }

    @Test
    void tipoForaDaListaDeveReprovar() {
        assertThat(validador.validar(new ClassificacaoBruta("NAO_INTERPRETADA", "x", "", "", "")))
                .isInstanceOf(Resultado.Invalida.class);
        assertThat(validador.validar(new ClassificacaoBruta("invente", "x", "", "", "")))
                .isInstanceOf(Resultado.Invalida.class);
    }

    @Test
    void periodoComFimAntesDoInicioDeveReprovar() {
        assertThat(validador.validar(new ClassificacaoBruta("NUMERICA", "x", "2025-03-31", "2025-03-01", "")))
                .isInstanceOf(Resultado.Invalida.class);
    }

    @Test
    void periodoPelaMetadeOuMalFormadoDeveReprovar() {
        assertThat(validador.validar(new ClassificacaoBruta("NUMERICA", "x", "2025-03-01", "", "")))
                .isInstanceOf(Resultado.Invalida.class);
        assertThat(validador.validar(new ClassificacaoBruta("NUMERICA", "x", "2025-02-31", "2025-03-01", "")))
                .isInstanceOf(Resultado.Invalida.class);
    }

    @Test
    void perguntaAutonomaVaziaDeveReprovar() {
        assertThat(validador.validar(new ClassificacaoBruta("DOCUMENTAL", "  ", "", "", "")))
                .isInstanceOf(Resultado.Invalida.class);
    }

    @Test
    void categoriaForaDaListaDeveReprovar() {
        assertThat(validador.validar(new ClassificacaoBruta("NUMERICA", "x", "", "", "CRIPTO")))
                .isInstanceOf(Resultado.Invalida.class);
    }
}
