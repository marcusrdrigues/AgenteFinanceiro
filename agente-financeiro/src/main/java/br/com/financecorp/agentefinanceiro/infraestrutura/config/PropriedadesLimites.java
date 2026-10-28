package br.com.financecorp.agentefinanceiro.infraestrutura.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Limites de consumo em código (constituição, "Restrições de Segurança e Dados"; FR-012). */
@Validated
@ConfigurationProperties("financeiro.limites")
public record PropriedadesLimites(
        @Min(1) int tamanhoMaximoPergunta,
        @Min(1) int requisicoesPorMinutoPorConversa,
        @Min(1) int requisicoesPorMinutoTotal,
        @Min(1) int chamadasFerramentaPorRequisicao) {
}
