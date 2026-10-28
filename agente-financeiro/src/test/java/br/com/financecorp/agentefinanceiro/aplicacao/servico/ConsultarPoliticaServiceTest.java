package br.com.financecorp.agentefinanceiro.aplicacao.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.financecorp.agentefinanceiro.aplicacao.contexto.MontadorContexto;
import br.com.financecorp.agentefinanceiro.aplicacao.excecao.SaidaModeloInvalidaException;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.Fonte;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.RespostaDocumentalRedigida;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.TrechoDocumento;
import br.com.financecorp.agentefinanceiro.aplicacao.porta.BuscaDocumentoPort;
import br.com.financecorp.agentefinanceiro.aplicacao.porta.RedatorDocumentalPort;
import br.com.financecorp.agentefinanceiro.aplicacao.servico.ConsultarPoliticaService.ResultadoPolitica;
import br.com.financecorp.agentefinanceiro.aplicacao.validacao.ValidadorReferencias;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConsultarPoliticaServiceTest {

    private final BuscaDocumentoPort busca = mock(BuscaDocumentoPort.class);
    private final RedatorDocumentalPort redator = mock(RedatorDocumentalPort.class);
    private final ConsultarPoliticaService servico = new ConsultarPoliticaService(
            busca, redator, new MontadorContexto(0.55, 1200, 4800), new ValidadorReferencias(), 2);

    private static final TrechoDocumento HOSPEDAGEM = new TrechoDocumento(UUID.randomUUID(),
            "Política de Reembolso de Despesas", "v2", "Hospedagem",
            "O limite diário de hospedagem é R$ 450,00.", 0.88);
    private static final TrechoDocumento ALIMENTACAO = new TrechoDocumento(UUID.randomUUID(),
            "Política de Reembolso de Alimentação", "v1", "Limites",
            "O limite diário de refeições em viagem é R$ 150,00.", 0.81);

    @Test
    void semTrechosDeveDevolverAMensagemFixaSemChamarOModelo() {
        when(busca.buscar(anyString())).thenReturn(List.of());

        ResultadoPolitica r = servico.consultar("Qual a política de investimentos?");

        assertThat(r.texto()).isEqualTo("Não encontrei essa informação nos documentos autorizados da FinanceCorp.");
        assertThat(r.fontes()).isEmpty();
        assertThat(r.ausencias()).isNotEmpty();
        assertThat(r.semFonte()).isTrue();
        verify(redator, never()).redigir(anyString(), any(), any());
    }

    @Test
    void trechosAbaixoDoLimiarContamComoSemFonte() {
        when(busca.buscar(anyString())).thenReturn(List.of(new TrechoDocumento(UUID.randomUUID(), "T", "v1", "S",
                "irrelevante", 0.20)));

        assertThat(servico.consultar("x").semFonte()).isTrue();
        verify(redator, never()).redigir(anyString(), any(), any());
    }

    @Test
    void respostaValidaDeveTrazerFontesDerivadasDaBusca() {
        when(busca.buscar(anyString())).thenReturn(List.of(HOSPEDAGEM));
        when(redator.redigir(anyString(), any(), any()))
                .thenReturn(new RespostaDocumentalRedigida("O limite é R$ 450,00 por dia.", true, List.of("T1")));

        ResultadoPolitica r = servico.consultar("Qual o limite de hospedagem?");

        assertThat(r.texto()).isEqualTo("O limite é R$ 450,00 por dia.");
        assertThat(r.fontes()).containsExactly(Fonte.documento("Política de Reembolso de Despesas", "v2", "Hospedagem"));
        assertThat(r.ausencias()).isEmpty();
        assertThat(r.documentosConsultados()).containsExactly("Política de Reembolso de Despesas@v2");
    }

    @Test
    void modeloSemConhecimentoSuficienteDeveVirarAMensagemFixa() {
        when(busca.buscar(anyString())).thenReturn(List.of(HOSPEDAGEM));
        when(redator.redigir(anyString(), any(), any()))
                .thenReturn(new RespostaDocumentalRedigida("Os trechos não tratam disso.", false, List.of()));

        assertThat(servico.consultar("x").texto()).isEqualTo(ConsultarPoliticaService.MENSAGEM_SEM_FONTE);
    }

    @Test
    void saidaInvalidaDeveSerRefeitaComOMotivoNoMaximoDuasVezes() {
        when(busca.buscar(anyString())).thenReturn(List.of(HOSPEDAGEM));
        when(redator.redigir(anyString(), any(), any()))
                .thenReturn(new RespostaDocumentalRedigida("O limite é R$ 999,00.", true, List.of("T1")));

        ResultadoPolitica r = servico.consultar("Qual o limite?");

        assertThat(r.texto()).isEqualTo(ConsultarPoliticaService.MENSAGEM_FALHA);
        assertThat(r.fontes()).isEmpty();
        verify(redator, times(3)).redigir(anyString(), any(), any());
        verify(redator, times(2)).redigir(anyString(), any(), org.mockito.ArgumentMatchers.argThat(
                (Optional<String> c) -> c.isPresent() && c.get().contains("R$ 999,00")));
    }

    @Test
    void saidaForaDoFormatoDeveSerRefeitaEDepoisAceita() {
        when(busca.buscar(anyString())).thenReturn(List.of(HOSPEDAGEM));
        when(redator.redigir(anyString(), any(), any()))
                .thenThrow(new SaidaModeloInvalidaException("JSON inválido"))
                .thenReturn(new RespostaDocumentalRedigida("Limite de R$ 450,00.", true, List.of("T1")));

        assertThat(servico.consultar("x").texto()).isEqualTo("Limite de R$ 450,00.");
        verify(redator, times(2)).redigir(anyString(), any(), any());
    }

    @Test
    void conflitoEntreDocumentosDeveCitarAsDuasFontes() {
        when(busca.buscar(anyString())).thenReturn(List.of(HOSPEDAGEM, ALIMENTACAO));
        when(redator.redigir(anyString(), any(), any())).thenReturn(new RespostaDocumentalRedigida(
                "Há dois limites: R$ 450,00 (hospedagem) e R$ 150,00 (refeições).", true, List.of("T1", "T2")));

        assertThat(servico.consultar("Quais os limites diários?").fontes()).containsExactly(
                Fonte.documento("Política de Reembolso de Despesas", "v2", "Hospedagem"),
                Fonte.documento("Política de Reembolso de Alimentação", "v1", "Limites"));
    }
}
