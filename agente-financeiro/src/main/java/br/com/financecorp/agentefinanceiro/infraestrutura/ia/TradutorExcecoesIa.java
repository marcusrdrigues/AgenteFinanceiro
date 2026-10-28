package br.com.financecorp.agentefinanceiro.infraestrutura.ia;

import com.openai.errors.BadRequestException;
import com.openai.errors.InternalServerException;
import com.openai.errors.NotFoundException;
import com.openai.errors.OpenAIException;
import com.openai.errors.OpenAIIoException;
import com.openai.errors.OpenAIRetryableException;
import com.openai.errors.OpenAIServiceException;
import com.openai.errors.PermissionDeniedException;
import com.openai.errors.RateLimitException;
import com.openai.errors.UnauthorizedException;
import com.openai.errors.UnprocessableEntityException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import org.springframework.stereotype.Component;

/**
 * Classifica falhas do provedor. No Spring AI 2.0.2 o modelo OpenAI usa o SDK oficial, que lança
 * {@code com.openai.errors.*}; o módulo spring-ai-retry (TransientAiException) nem está no classpath.
 */
@Component
public final class TradutorExcecoesIa {

    public enum Natureza {
        /** Limite de requisições, erro do servidor, timeout: pode repetir. */
        TRANSITORIA,
        /** Requisição ou credencial errada: repetir não adianta. */
        NAO_TRANSITORIA,
        /** Não é falha do provedor (ex.: conversão da saída). */
        NAO_E_DO_PROVEDOR
    }

    public Natureza classificar(Throwable falha) {
        for (Throwable atual = falha; atual != null; atual = atual.getCause()) {
            Natureza natureza = classificarUma(atual);
            if (natureza != Natureza.NAO_E_DO_PROVEDOR) {
                return natureza;
            }
            if (atual.getCause() == atual) {
                break;
            }
        }
        return Natureza.NAO_E_DO_PROVEDOR;
    }

    public boolean ehTransitoria(Throwable falha) {
        return classificar(falha) == Natureza.TRANSITORIA;
    }

    /** 401 e 403 indicam configuração errada da chave, e não instabilidade. */
    public boolean ehFalhaDeCredencial(Throwable falha) {
        for (Throwable atual = falha; atual != null && atual.getCause() != atual; atual = atual.getCause()) {
            if (atual instanceof UnauthorizedException || atual instanceof PermissionDeniedException) {
                return true;
            }
        }
        return false;
    }

    private static Natureza classificarUma(Throwable t) {
        if (t instanceof RateLimitException
                || t instanceof InternalServerException || t instanceof OpenAIIoException
                || t instanceof OpenAIRetryableException || t instanceof InterruptedIOException
                || t instanceof ConnectException) {
            return Natureza.TRANSITORIA;
        }
        if (t instanceof UnauthorizedException
                || t instanceof PermissionDeniedException || t instanceof BadRequestException
                || t instanceof NotFoundException || t instanceof UnprocessableEntityException) {
            return Natureza.NAO_TRANSITORIA;
        }
        if (t instanceof OpenAIServiceException servico) {
            int status = servico.statusCode();
            return status == 408 || status == 409 || status == 429 || status >= 500
                    ? Natureza.TRANSITORIA
                    : Natureza.NAO_TRANSITORIA;
        }
        if (t instanceof OpenAIException) {
            return Natureza.NAO_TRANSITORIA;
        }
        return Natureza.NAO_E_DO_PROVEDOR;
    }
}
