package br.com.financecorp.agentefinanceiro.interfaces.web;

import br.com.financecorp.agentefinanceiro.dominio.excecao.CategoriaInvalidaException;
import br.com.financecorp.agentefinanceiro.dominio.excecao.ConversaIdInvalidoException;
import br.com.financecorp.agentefinanceiro.dominio.excecao.ExcecaoDominio;
import br.com.financecorp.agentefinanceiro.dominio.excecao.LimiteRequisicoesException;
import br.com.financecorp.agentefinanceiro.dominio.excecao.PerguntaInvalidaException;
import br.com.financecorp.agentefinanceiro.dominio.excecao.PeriodoInvalidoException;
import br.com.financecorp.agentefinanceiro.dominio.excecao.ServicoIndisponivelException;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Traduz exceções em ProblemDetail (RFC 9457). Nenhuma resposta leva stack trace, nome de classe,
 * SQL ou o valor recebido (FR-011): o título é fixo por código e o {@code instance} é o traceId,
 * não o caminho da requisição (que conteria o conversaId enviado pelo cliente).
 */
@RestControllerAdvice
public class ErroHandler extends ResponseEntityExceptionHandler {

    static final String BASE_TIPO = "https://financecorp.example/problemas/";
    private static final Logger LOG = LoggerFactory.getLogger(ErroHandler.class);

    @ExceptionHandler(ExcecaoDominio.class)
    ResponseEntity<ProblemDetail> tratarDominio(ExcecaoDominio excecao) {
        HttpStatus status = statusDe(excecao);
        if (status == HttpStatus.INTERNAL_SERVER_ERROR) {
            // Situações funcionais (não interpretada, especialista indisponível) são tratadas no caso
            // de uso e viram 200; chegar aqui é defeito de programação.
            LOG.error("Exceção de domínio não tratada no caso de uso: {}", excecao.codigo());
            return resposta(problema(HttpStatus.INTERNAL_SERVER_ERROR, "erro-interno", "Erro interno."), new HttpHeaders());
        }
        HttpHeaders cabecalhos = new HttpHeaders();
        if (excecao instanceof ServicoIndisponivelException indisponivel) {
            cabecalhos.set(HttpHeaders.RETRY_AFTER, String.valueOf(indisponivel.segundosParaNovaTentativa()));
        }
        return resposta(problema(status, excecao.codigo(), excecao.getMessage()), cabecalhos);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> tratarInesperada(Exception excecao) {
        LOG.error("Erro inesperado", excecao);
        return resposta(problema(HttpStatus.INTERNAL_SERVER_ERROR, "erro-interno", "Erro interno."), new HttpHeaders());
    }

    /** Erros do próprio Spring MVC (404, 405, corpo ilegível, validação): mesmo formato, sem o detalhe original. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception excecao, Object corpo, HttpHeaders cabecalhos, HttpStatusCode status, WebRequest requisicao) {
        HttpStatus conhecido = HttpStatus.resolve(status.value());
        HttpStatus efetivo = conhecido != null ? conhecido : HttpStatus.BAD_REQUEST;
        ProblemDetail problema = problema(efetivo, codigoPadrao(efetivo), tituloPadrao(efetivo));
        HttpHeaders limpos = new HttpHeaders();
        limpos.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        return new ResponseEntity<>(problema, limpos, efetivo);
    }

    private static HttpStatus statusDe(ExcecaoDominio excecao) {
        return switch (excecao) {
            case ConversaIdInvalidoException e -> HttpStatus.BAD_REQUEST;
            case PeriodoInvalidoException e -> HttpStatus.BAD_REQUEST;
            case CategoriaInvalidaException e -> HttpStatus.BAD_REQUEST;
            case PerguntaInvalidaException e -> e.motivo() == PerguntaInvalidaException.Motivo.MUITO_LONGA
                    ? HttpStatus.CONTENT_TOO_LARGE
                    : HttpStatus.BAD_REQUEST;
            case LimiteRequisicoesException e -> HttpStatus.TOO_MANY_REQUESTS;
            case ServicoIndisponivelException e -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    private static ProblemDetail problema(HttpStatus status, String codigo, String titulo) {
        ProblemDetail problema = ProblemDetail.forStatus(status);
        problema.setType(URI.create(BASE_TIPO + codigo));
        problema.setTitle(titulo);
        String traceId = MDC.get(FiltroTraceId.CHAVE_MDC);
        if (traceId != null) {
            problema.setProperty("traceId", traceId);
            problema.setInstance(URI.create("urn:financecorp:requisicao:" + traceId));
        } else {
            problema.setInstance(URI.create("urn:financecorp:requisicao"));
        }
        return problema;
    }

    private static ResponseEntity<ProblemDetail> resposta(ProblemDetail problema, HttpHeaders cabecalhos) {
        cabecalhos.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        return new ResponseEntity<>(problema, cabecalhos, problema.getStatus());
    }

    private static String codigoPadrao(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "recurso-nao-encontrado";
            case METHOD_NOT_ALLOWED -> "metodo-nao-permitido";
            case UNSUPPORTED_MEDIA_TYPE, NOT_ACCEPTABLE -> "formato-nao-suportado";
            case CONTENT_TOO_LARGE -> "requisicao-muito-grande";
            default -> status.is5xxServerError() ? "erro-interno" : "requisicao-invalida";
        };
    }

    private static String tituloPadrao(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "Recurso não encontrado.";
            case METHOD_NOT_ALLOWED -> "Método não permitido.";
            case UNSUPPORTED_MEDIA_TYPE, NOT_ACCEPTABLE -> "Formato não suportado.";
            case CONTENT_TOO_LARGE -> "Requisição muito grande.";
            default -> status.is5xxServerError() ? "Erro interno." : "Requisição inválida.";
        };
    }
}
