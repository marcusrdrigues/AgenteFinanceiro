package br.com.financecorp.agentefinanceiro.aplicacao.modelo;

import br.com.financecorp.agentefinanceiro.dominio.TipoResposta;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Metadados de uma requisição (FR-036, FR-037). Não há campo para texto de pergunta, de resposta ou
 * valor monetário: o que não pode ir para o log não tem onde ser posto.
 *
 * @param documentosConsultados {@code titulo@versao}
 * @param consultasNumericas    {@code ferramenta:inicio..fim:categoria}, sem valores
 */
public record RegistroExecucao(
        String traceId,
        UUID conversaId,
        TipoResposta tipo,
        long duracaoMs,
        Integer tokensEntrada,
        Integer tokensSaida,
        Map<String, String> versoesPrompt,
        List<String> documentosConsultados,
        List<String> consultasNumericas,
        UUID solicitacaoEspecialistaId,
        String resultadoEspecialista,
        String resultado,
        String codigoErro) {

    public RegistroExecucao {
        Objects.requireNonNull(traceId, "traceId");
        Objects.requireNonNull(resultado, "resultado");
        versoesPrompt = versoesPrompt == null ? Map.of() : Map.copyOf(versoesPrompt);
        documentosConsultados = documentosConsultados == null ? List.of() : List.copyOf(documentosConsultados);
        consultasNumericas = consultasNumericas == null ? List.of() : List.copyOf(consultasNumericas);
    }
}
