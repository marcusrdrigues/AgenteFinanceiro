package br.com.financecorp.agentefinanceiro.interfaces.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.financecorp.agentefinanceiro.aplicacao.modelo.Fonte;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.RespostaAssistente;
import br.com.financecorp.agentefinanceiro.aplicacao.servico.ResponderPerguntaService;
import br.com.financecorp.agentefinanceiro.dominio.ConversaId;
import br.com.financecorp.agentefinanceiro.dominio.TipoResposta;
import br.com.financecorp.agentefinanceiro.dominio.excecao.ConversaIdInvalidoException;
import br.com.financecorp.agentefinanceiro.dominio.excecao.ModeloIndisponivelException;
import br.com.financecorp.agentefinanceiro.dominio.excecao.PerguntaInvalidaException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(PerguntaController.class)
@TestPropertySource(properties = {
    "financeiro.limites.requisicoes-por-minuto-por-conversa=3",
    "financeiro.limites.requisicoes-por-minuto-total=1000"
})
class PerguntaControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ResponderPerguntaService servico;

    @Test
    void deveResponder200NoFormatoDoContrato() throws Exception {
        String conversa = UUID.randomUUID().toString();
        when(servico.responder(eq(conversa), anyString(), any())).thenReturn(new RespostaAssistente(ConversaId.de(conversa),
                TipoResposta.DOCUMENTAL, "O limite é R$ 450,00.", List.of(), null,
                List.of(Fonte.documento("Política de Reembolso", "v2", "Hospedagem")), List.of()));

        perguntar(conversa, "{\"pergunta\":\"Qual o limite de hospedagem?\"}")
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().exists(FiltroTraceId.CABECALHO))
                .andExpect(jsonPath("$.conversaId").value(conversa))
                .andExpect(jsonPath("$.tipo").value("DOCUMENTAL"))
                .andExpect(jsonPath("$.texto").value("O limite é R$ 450,00."))
                .andExpect(jsonPath("$.valores").isArray())
                .andExpect(jsonPath("$.fontes[0].tipo").value("DOCUMENTO"))
                .andExpect(jsonPath("$.fontes[0].titulo").value("Política de Reembolso"))
                .andExpect(jsonPath("$.fontes[0].versao").value("v2"))
                .andExpect(jsonPath("$.fontes[0].secao").value("Hospedagem"))
                .andExpect(jsonPath("$.ausencias").isArray())
                .andExpect(content().string(not(containsString("T1"))))
                .andExpect(content().string(not(containsString("prompt"))))
                .andExpect(content().string(not(containsString("token"))));
    }

    @Test
    void conversaIdForaDoFormatoDeveSer400() throws Exception {
        when(servico.responder(eq("123"), anyString(), any())).thenThrow(new ConversaIdInvalidoException());

        perguntar("123", "{\"pergunta\":\"oi\"}")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(content().string(not(containsString("123/perguntas"))));
    }

    @Test
    void corpoComCampoExtraOuSemPerguntaDeveSer400() throws Exception {
        String conversa = UUID.randomUUID().toString();
        perguntar(conversa, "{\"pergunta\":\"oi\",\"modelo\":\"outro\"}").andExpect(status().isBadRequest());
        perguntar(conversa, "{}").andExpect(status().isBadRequest());
        perguntar(conversa, "não é json").andExpect(status().isBadRequest());
    }

    @Test
    void perguntaVaziaDeveSer400ELongaDemaisDeveSer413() throws Exception {
        String conversa = UUID.randomUUID().toString();
        when(servico.responder(eq(conversa), eq(""), any()))
                .thenThrow(new PerguntaInvalidaException(PerguntaInvalidaException.Motivo.VAZIA));
        when(servico.responder(eq(conversa), eq("a".repeat(1001)), any()))
                .thenThrow(new PerguntaInvalidaException(PerguntaInvalidaException.Motivo.MUITO_LONGA));

        perguntar(conversa, "{\"pergunta\":\"\"}").andExpect(status().isBadRequest());
        perguntar(conversa, "{\"pergunta\":\"" + "a".repeat(1001) + "\"}").andExpect(status().isPayloadTooLarge());
    }

    @Test
    void provedorForaDoArDeveSer503ComRetryAfter() throws Exception {
        String conversa = UUID.randomUUID().toString();
        when(servico.responder(eq(conversa), anyString(), any())).thenThrow(new ModeloIndisponivelException());

        perguntar(conversa, "{\"pergunta\":\"oi\"}")
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void acimaDoLimitePorConversaDeveSer429ComRetryAfter() throws Exception {
        String conversa = UUID.randomUUID().toString();
        when(servico.responder(eq(conversa), anyString(), any())).thenReturn(new RespostaAssistente(ConversaId.de(conversa),
                TipoResposta.FORA_DO_DOMINIO, "x", List.of(), null, List.of(), List.of()));
        for (int i = 0; i < 3; i++) {
            perguntar(conversa, "{\"pergunta\":\"oi\"}").andExpect(status().isOk());
        }

        perguntar(conversa, "{\"pergunta\":\"oi\"}")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(ErroHandler.BASE_TIPO + "limite-requisicoes"));
        // Outra conversa não é afetada.
        String outra = UUID.randomUUID().toString();
        when(servico.responder(eq(outra), anyString(), any())).thenReturn(new RespostaAssistente(ConversaId.de(outra),
                TipoResposta.FORA_DO_DOMINIO, "x", List.of(), null, List.of(), List.of()));
        perguntar(outra, "{\"pergunta\":\"oi\"}").andExpect(status().isOk());
    }

    @Test
    void caminhoSemIdentificadorDeveSer404EmProblemDetail() throws Exception {
        mvc.perform(post("/api/v1/conversas//perguntas").contentType(MediaType.APPLICATION_JSON).content("{\"pergunta\":\"oi\"}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    private ResultActions perguntar(String conversa, String corpo) throws Exception {
        return mvc.perform(post("/api/v1/conversas/" + conversa + "/perguntas")
                .contentType(MediaType.APPLICATION_JSON).content(corpo));
    }
}
