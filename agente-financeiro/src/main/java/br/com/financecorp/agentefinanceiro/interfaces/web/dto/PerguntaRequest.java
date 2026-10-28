package br.com.financecorp.agentefinanceiro.interfaces.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Corpo da pergunta. Vazio (400) e acima de 1.000 caracteres (413) são verificados no caso de uso,
 * porque o contrato pede 413 para tamanho; aqui fica só a presença do campo. Propriedade desconhecida
 * é recusada pela configuração do Jackson (spring.jackson.deserialization).
 */
@Schema(description = "Pergunta em português do Brasil")
public record PerguntaRequest(
        @NotNull @Schema(description = "Texto da pergunta, de 1 a 1.000 caracteres", example = "Qual o limite diário de hospedagem?",
                requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 1000) String pergunta) {
}
