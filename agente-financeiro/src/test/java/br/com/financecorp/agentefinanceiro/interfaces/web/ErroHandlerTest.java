package br.com.financecorp.agentefinanceiro.interfaces.web;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.financecorp.agentefinanceiro.dominio.excecao.CategoriaInvalidaException;
import br.com.financecorp.agentefinanceiro.dominio.excecao.ConversaIdInvalidoException;
import br.com.financecorp.agentefinanceiro.dominio.excecao.EspecialistaIndisponivelException;
import br.com.financecorp.agentefinanceiro.dominio.excecao.FonteDadosIndisponivelException;
import br.com.financecorp.agentefinanceiro.dominio.excecao.LimiteRequisicoesException;
import br.com.financecorp.agentefinanceiro.dominio.excecao.ModeloIndisponivelException;
import br.com.financecorp.agentefinanceiro.dominio.excecao.PerguntaInvalidaException;
import br.com.financecorp.agentefinanceiro.dominio.excecao.PerguntaNaoInterpretadaException;
import br.com.financecorp.agentefinanceiro.dominio.excecao.PeriodoInvalidoException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = ErroHandlerTest.ControladorDeTeste.class)
@Import({ErroHandler.class, FiltroTraceId.class, ErroHandlerTest.ControladorDeTeste.class})
class ErroHandlerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void conversaIdInvalidoDeveSer400() throws Exception {
        problema(get("/teste/erro/conversa"), 400, "conversa-id-invalido");
    }

    @Test
    void periodoECategoriaInvalidosDevemSer400() throws Exception {
        problema(get("/teste/erro/periodo"), 400, "periodo-invalido");
        problema(get("/teste/erro/categoria"), 400, "categoria-invalida");
    }

    @Test
    void perguntaVaziaDeveSer400EMuitoLongaDeveSer413() throws Exception {
        problema(get("/teste/erro/pergunta-vazia"), 400, "pergunta-vazia");
        problema(get("/teste/erro/pergunta-longa"), 413, "pergunta-muito-longa");
    }

    @Test
    void limiteDeveSer429ComRetryAfter() throws Exception {
        problema(get("/teste/erro/limite"), 429, "limite-requisicoes")
                .andExpect(header().string("Retry-After", "7"));
    }

    @Test
    void indisponibilidadeDeveSer503ComRetryAfter() throws Exception {
        problema(get("/teste/erro/modelo"), 503, "modelo-indisponivel")
                .andExpect(header().string("Retry-After", "30"));
        problema(get("/teste/erro/fonte"), 503, "fonte-dados-indisponivel")
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void situacaoFuncionalQueEscapaDoCasoDeUsoDeveSer500Generico() throws Exception {
        problema(get("/teste/erro/nao-interpretada"), 500, "erro-interno");
        problema(get("/teste/erro/especialista"), 500, "erro-interno");
    }

    @Test
    void erroInesperadoNaoDeveExporDetalheInterno() throws Exception {
        problema(get("/teste/erro/inesperado"), 500, "erro-interno")
                .andExpect(content().string(not(containsString("SELECT"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))))
                .andExpect(content().string(not(containsString("at br.com"))));
    }

    @Test
    void corpoInvalidoDeveSer400SemEcoarOValor() throws Exception {
        problema(post("/teste/validacao").contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"\"}"),
                400, "requisicao-invalida");
        problema(post("/teste/validacao").contentType(MediaType.APPLICATION_JSON).content("{\"texto\": segredo-mal-formado"),
                400, "requisicao-invalida")
                .andExpect(content().string(not(containsString("segredo-mal-formado"))));
    }

    @Test
    void rotaInexistenteDeveSer404SemEcoarOCaminho() throws Exception {
        problema(get("/teste/caminho-que-nao-existe"), 404, "recurso-nao-encontrado")
                .andExpect(content().string(not(containsString("caminho-que-nao-existe"))));
    }

    @Test
    void problemaDeveTrazerOTraceIdDoCabecalho() throws Exception {
        String traceId = mvc.perform(get("/teste/erro/conversa"))
                .andReturn().getResponse().getHeader(FiltroTraceId.CABECALHO);
        mvc.perform(get("/teste/erro/conversa"))
                .andExpect(jsonPath("$.traceId").isString())
                .andExpect(jsonPath("$.instance", startsWith("urn:financecorp:requisicao:")));
        org.assertj.core.api.Assertions.assertThat(traceId).isNotBlank();
    }

    private ResultActions problema(org.springframework.test.web.servlet.RequestBuilder requisicao, int status, String codigo)
            throws Exception {
        return mvc.perform(requisicao)
                .andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(ErroHandler.BASE_TIPO + codigo))
                .andExpect(jsonPath("$.title").isString())
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.detail").doesNotExist())
                .andExpect(content().string(not(containsString("Exception"))))
                .andExpect(content().string(not(containsString("trace\""))));
    }

    record CorpoDeTeste(@NotBlank String texto) {
    }

    @RestController
    static class ControladorDeTeste {

        @GetMapping("/teste/erro/{caso}")
        void erro(@PathVariable String caso) {
            throw switch (caso) {
                case "conversa" -> new ConversaIdInvalidoException();
                case "periodo" -> new PeriodoInvalidoException("a data final é anterior à inicial");
                case "categoria" -> new CategoriaInvalidaException();
                case "pergunta-vazia" -> new PerguntaInvalidaException(PerguntaInvalidaException.Motivo.VAZIA);
                case "pergunta-longa" -> new PerguntaInvalidaException(PerguntaInvalidaException.Motivo.MUITO_LONGA);
                case "limite" -> new LimiteRequisicoesException(7);
                case "modelo" -> new ModeloIndisponivelException();
                case "fonte" -> new FonteDadosIndisponivelException();
                case "nao-interpretada" -> new PerguntaNaoInterpretadaException();
                case "especialista" -> new EspecialistaIndisponivelException();
                default -> new IllegalStateException("SELECT * FROM financeiro.lancamento");
            };
        }

        @PostMapping("/teste/validacao")
        void validar(@Valid @RequestBody CorpoDeTeste corpo) {
        }
    }
}
