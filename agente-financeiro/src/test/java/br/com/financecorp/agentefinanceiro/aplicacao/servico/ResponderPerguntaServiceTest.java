package br.com.financecorp.agentefinanceiro.aplicacao.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.financecorp.agentefinanceiro.aplicacao.contexto.MontadorContexto;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.ClassificacaoBruta;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.RegistroExecucao;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.RespostaAssistente;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.RespostaDocumentalRedigida;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.TrechoDocumento;
import br.com.financecorp.agentefinanceiro.aplicacao.porta.BuscaDocumentoPort;
import br.com.financecorp.agentefinanceiro.aplicacao.porta.ClassificadorPerguntaPort;
import br.com.financecorp.agentefinanceiro.aplicacao.porta.RedatorDocumentalPort;
import br.com.financecorp.agentefinanceiro.aplicacao.porta.RegistroExecucaoPort;
import br.com.financecorp.agentefinanceiro.aplicacao.validacao.ValidadorClassificacao;
import br.com.financecorp.agentefinanceiro.aplicacao.validacao.ValidadorReferencias;
import br.com.financecorp.agentefinanceiro.dominio.TipoResposta;
import br.com.financecorp.agentefinanceiro.dominio.excecao.ConversaIdInvalidoException;
import br.com.financecorp.agentefinanceiro.dominio.excecao.ModeloIndisponivelException;
import br.com.financecorp.agentefinanceiro.dominio.excecao.PerguntaInvalidaException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ResponderPerguntaServiceTest {

    private static final String CONVERSA = UUID.randomUUID().toString();

    private final ClassificadorPerguntaPort classificador = mock(ClassificadorPerguntaPort.class);
    private final BuscaDocumentoPort busca = mock(BuscaDocumentoPort.class);
    private final RedatorDocumentalPort redator = mock(RedatorDocumentalPort.class);
    private final List<RegistroExecucao> registros = new ArrayList<>();
    private final RegistroExecucaoPort registro = registros::add;
    private final ResponderPerguntaService servico = servico(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));

    private ResponderPerguntaService servico(Clock relogio) {
        ConsultarPoliticaService politicas = new ConsultarPoliticaService(busca, redator,
                new MontadorContexto(0.55, 1200, 4800), new ValidadorReferencias(), 2);
        return new ResponderPerguntaService(classificador, new ValidadorClassificacao(1000), politicas, registro,
                relogio, new ResponderPerguntaService.Parametros(2, Duration.ofSeconds(25), 1000,
                        Map.of("classificar", "v1")));
    }

    @Test
    void perguntaDocumentalDeveSeguirOCaminhoDePoliticas() {
        classificarComo("DOCUMENTAL", "Qual o limite de hospedagem?");
        when(busca.buscar("Qual o limite de hospedagem?")).thenReturn(List.of(new TrechoDocumento(UUID.randomUUID(),
                "Política de Reembolso", "v2", "Hospedagem", "Limite de R$ 450,00.", 0.9)));
        when(redator.redigir(anyString(), any(), any()))
                .thenReturn(new RespostaDocumentalRedigida("O limite é R$ 450,00.", true, List.of("T1")));

        RespostaAssistente r = servico.responder(CONVERSA, "limite de hospedagem?", "trace-1");

        assertThat(r.tipo()).isEqualTo(TipoResposta.DOCUMENTAL);
        assertThat(r.fontes()).hasSize(1);
        assertThat(r.conversaId().toString()).isEqualTo(CONVERSA);
    }

    @Test
    void foraDoDominioDeveRecusarSemBuscarNemRedigir() {
        classificarComo("FORA_DO_DOMINIO", "Vai chover amanhã?");

        RespostaAssistente r = servico.responder(CONVERSA, "Vai chover amanhã?", "t");

        assertThat(r.tipo()).isEqualTo(TipoResposta.FORA_DO_DOMINIO);
        assertThat(r.texto()).isEqualTo(ResponderPerguntaService.MENSAGEM_FORA_DO_DOMINIO);
        verify(busca, never()).buscar(anyString());
        verify(redator, never()).redigir(anyString(), any(), any());
    }

    @Test
    void classificacaoInvalidaTresVezesDeveVirarNaoInterpretada() {
        when(classificador.classificar(anyString(), any(), any()))
                .thenReturn(new ClassificacaoBruta("INVENTADO", "x", "", "", ""));

        RespostaAssistente r = servico.responder(CONVERSA, "???", "t");

        assertThat(r.tipo()).isEqualTo(TipoResposta.NAO_INTERPRETADA);
        assertThat(r.texto()).isEqualTo(ResponderPerguntaService.MENSAGEM_NAO_INTERPRETADA);
        verify(classificador, times(3)).classificar(anyString(), any(), any());
    }

    @Test
    void tiposAindaNaoImplementadosDevemInformarAusencia() {
        for (String tipo : List.of("NUMERICA", "MISTA", "ANALISE", "RISCO_ESPECIALIZADO")) {
            classificarComo(tipo, "pergunta");
            RespostaAssistente r = servico.responder(CONVERSA, "pergunta", "t");
            assertThat(r.ausencias()).containsExactly("funcionalidade ainda não disponível nesta etapa");
            assertThat(r.valores()).isEmpty();
        }
        verify(busca, never()).buscar(anyString());
    }

    @Test
    void cadaRequisicaoDeveGerarExatamenteUmRegistroDeExecucao() {
        classificarComo("DOCUMENTAL", "Qual o limite?");
        when(busca.buscar(anyString())).thenReturn(List.of());

        servico.responder(CONVERSA, "Qual o limite?", "trace-42");

        assertThat(registros).hasSize(1);
        RegistroExecucao r = registros.getFirst();
        assertThat(r.traceId()).isEqualTo("trace-42");
        assertThat(r.conversaId().toString()).isEqualTo(CONVERSA);
        assertThat(r.tipo()).isEqualTo(TipoResposta.DOCUMENTAL);
        assertThat(r.resultado()).isEqualTo("sem-fonte");
        assertThat(r.versoesPrompt()).containsEntry("classificar", "v1");
    }

    @Test
    void erroTambemDeveGerarRegistroComOCodigo() {
        assertThatThrownBy(() -> servico.responder("123", "pergunta", "trace-erro"))
                .isInstanceOf(ConversaIdInvalidoException.class);
        assertThat(registros).singleElement().satisfies(r -> {
            assertThat(r.resultado()).isEqualTo("erro");
            assertThat(r.codigoErro()).isEqualTo("conversa-id-invalido");
        });
    }

    @Test
    void perguntaVaziaOuLongaDemaisDeveSerRecusada() {
        assertThatThrownBy(() -> servico.responder(CONVERSA, "  ", "t"))
                .isInstanceOfSatisfying(PerguntaInvalidaException.class,
                        e -> assertThat(e.motivo()).isEqualTo(PerguntaInvalidaException.Motivo.VAZIA));
        assertThatThrownBy(() -> servico.responder(CONVERSA, "a".repeat(1001), "t"))
                .isInstanceOfSatisfying(PerguntaInvalidaException.class,
                        e -> assertThat(e.motivo()).isEqualTo(PerguntaInvalidaException.Motivo.MUITO_LONGA));
        classificarComo("FORA_DO_DOMINIO", "x");
        assertThat(servico.responder(CONVERSA, "a".repeat(1000), "t")).isNotNull();
    }

    @Test
    void prazoEstouradoDeveInterromperAntesDeRedigir() {
        Clock relogioQueAnda = new Clock() {
            private Instant agora = Instant.EPOCH;

            @Override
            public Instant instant() {
                Instant atual = agora;
                agora = agora.plusSeconds(30);
                return atual;
            }

            @Override
            public java.time.ZoneId getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(java.time.ZoneId zona) {
                return this;
            }
        };
        classificarComo("DOCUMENTAL", "x");

        assertThatThrownBy(() -> servico(relogioQueAnda).responder(CONVERSA, "x", "t"))
                .isInstanceOf(ModeloIndisponivelException.class);
        verify(busca, never()).buscar(anyString());
    }

    private void classificarComo(String tipo, String autonoma) {
        when(classificador.classificar(anyString(), any(), any()))
                .thenReturn(new ClassificacaoBruta(tipo, autonoma, "", "", ""));
    }
}
