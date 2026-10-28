package br.com.financecorp.agentefinanceiro.aplicacao.validacao;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.financecorp.agentefinanceiro.aplicacao.contexto.MontadorContexto;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.ContextoMontado;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.Fonte;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.RespostaDocumentalRedigida;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.TrechoDocumento;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ValidadorReferenciasTest {

    private final ValidadorReferencias validador = new ValidadorReferencias();
    private final ContextoMontado contexto = new MontadorContexto(0.0, 1200, 4800).montar(List.of(
            trecho("Política de Reembolso", "Hospedagem", "O limite diário de hospedagem é R$ 450,00.", 0.91),
            trecho("Política de Alimentação", "Limites", "O limite diário de alimentação é R$ 120,00.", 0.80),
            trecho("Política de Reembolso", "Hospedagem", "Hospedagem acima do limite exige aprovação.", 0.70)));

    @Test
    void deveAprovarRespostaComReferenciaEValorPresenteNoTrecho() {
        var resposta = new RespostaDocumentalRedigida("O limite é R$ 450,00 por dia.", true, List.of("T1"));
        assertThat(validador.validar(resposta, contexto).aprovado()).isTrue();
    }

    @Test
    void deveReprovarReferenciaInexistente() {
        var resposta = new RespostaDocumentalRedigida("Resposta.", true, List.of("T7"));
        assertThat(validador.validar(resposta, contexto).motivoReprovacao()).hasValueSatisfying(
                m -> assertThat(m).contains("T7"));
    }

    @Test
    void deveReprovarConhecimentoSuficienteSemReferencia() {
        var resposta = new RespostaDocumentalRedigida("Resposta sem citar.", true, List.of());
        assertThat(validador.validar(resposta, contexto).aprovado()).isFalse();
    }

    @Test
    void deveReprovarValorQueNaoEstaNoTrechoReferenciado() {
        var inventado = new RespostaDocumentalRedigida("O limite é R$ 500,00.", true, List.of("T1"));
        var deOutroTrecho = new RespostaDocumentalRedigida("O limite é R$ 120,00.", true, List.of("T1"));
        assertThat(validador.validar(inventado, contexto).aprovado()).isFalse();
        assertThat(validador.validar(deOutroTrecho, contexto).aprovado()).isFalse();
    }

    @Test
    void deveAceitarValorEscritoSemCentavosQuandoOTrechoTemCentavos() {
        var resposta = new RespostaDocumentalRedigida("O limite é R$ 450.", true, List.of("T1"));
        assertThat(validador.validar(resposta, contexto).aprovado()).isTrue();
    }

    @Test
    void fontesDevemVirDosTrechosSemRepeticaoEEmOrdemDeSimilaridade() {
        var resposta = new RespostaDocumentalRedigida("Texto.", true, List.of("T3", "T2", "T1", "T1"));
        assertThat(validador.fontes(resposta, contexto)).containsExactly(
                Fonte.documento("Política de Reembolso", "v1", "Hospedagem"),
                Fonte.documento("Política de Alimentação", "v1", "Limites"));
    }

    private static TrechoDocumento trecho(String titulo, String secao, String texto, double similaridade) {
        return new TrechoDocumento(UUID.randomUUID(), titulo, "v1", secao, texto, similaridade);
    }
}
