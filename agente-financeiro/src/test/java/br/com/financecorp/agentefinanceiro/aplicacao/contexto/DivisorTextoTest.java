package br.com.financecorp.agentefinanceiro.aplicacao.contexto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.financecorp.agentefinanceiro.aplicacao.excecao.DocumentoGrandeDemaisException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class DivisorTextoTest {

    private final DivisorTexto divisor = new DivisorTexto(1200, 200, 100, 200);

    @Test
    void textoCurtoDeveVirarUmTrecho() {
        assertThat(divisor.dividir("  Política curta.  ")).containsExactly("Política curta.");
    }

    @Test
    void textoVazioNaoGeraTrechos() {
        assertThat(divisor.dividir("   ")).isEmpty();
    }

    @Test
    void nenhumTrechoDevePassarDoMaximo() {
        List<String> trechos = divisor.dividir(frases(200));
        assertThat(trechos).hasSizeGreaterThan(1).allSatisfy(t -> assertThat(t.length()).isLessThanOrEqualTo(1200));
    }

    @Test
    void vizinhosDevemSeSobrepor() {
        List<String> trechos = divisor.dividir(frases(200));
        for (int i = 0; i < trechos.size() - 1; i++) {
            String inicioDoProximo = trechos.get(i + 1).substring(0, 80);
            assertThat(trechos.get(i)).contains(inicioDoProximo);
        }
    }

    @Test
    void devePreferirCortarNoFimDoParagrafo() {
        String paragrafo1 = "a".repeat(10) + " " + frases(15);
        String texto = paragrafo1 + "\n\n" + frases(30);
        List<String> trechos = divisor.dividir(texto);
        assertThat(trechos.getFirst()).isEqualTo(paragrafo1.strip());
    }

    @Test
    void deveCortarNoFimDeFraseQuandoNaoHaParagrafo() {
        List<String> trechos = divisor.dividir(frases(200));
        assertThat(trechos.getFirst()).endsWith(".");
    }

    @Test
    void mesmaEntradaDeveGerarMesmosTrechos() {
        String texto = frases(150);
        assertThat(divisor.dividir(texto)).isEqualTo(divisor.dividir(texto));
    }

    @Test
    void ultimoTrechoMenorQueOMinimoDeveSerDescartadoQuandoHaAnterior() {
        DivisorTexto pequeno = new DivisorTexto(300, 50, 100, 200);
        List<String> trechos = pequeno.dividir(frases(9) + " Fim.");
        assertThat(trechos).allSatisfy(t -> assertThat(t.length()).isGreaterThanOrEqualTo(100));
    }

    @Test
    void documentoComTrechosDemaisDeveFalhar() {
        DivisorTexto limitado = new DivisorTexto(200, 20, 10, 3);
        assertThatThrownBy(() -> limitado.dividir(frases(100))).isInstanceOf(DocumentoGrandeDemaisException.class);
    }

    @Test
    void sobreposicaoMaiorOuIgualAoTrechoDeveSerRecusada() {
        assertThatThrownBy(() -> new DivisorTexto(200, 200, 10, 10)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void textoSemEspacosNaoDeveEntrarEmLaco() {
        String semEspacos = "x".repeat(5000);
        List<String> trechos = org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(
                Duration.ofSeconds(2), () -> divisor.dividir(semEspacos));
        assertThat(trechos).hasSizeGreaterThan(3).allSatisfy(t -> assertThat(t.length()).isLessThanOrEqualTo(1200));
    }

    private static String frases(int quantidade) {
        StringBuilder texto = new StringBuilder();
        for (int i = 1; i <= quantidade; i++) {
            texto.append("Esta é a frase número ").append(i).append(" da política fictícia. ");
        }
        return texto.toString().strip();
    }
}
