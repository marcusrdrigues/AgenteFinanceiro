package br.com.financecorp.agentefinanceiro.infraestrutura.config;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Parâmetros do uso do modelo; valores e motivos em application.yml (research D9 e D14). */
@Validated
@ConfigurationProperties("financeiro.ia")
public record PropriedadesIa(
        @Min(0) @Max(2) int tentativasValidacao,
        @Min(0) @Max(3) int tentativasTransitorias,
        @NotNull Duration backoffInicial,
        @DecimalMin("1.0") double multiplicadorBackoff,
        @NotNull Duration tempoTotal,
        @NotNull Duration tempoTotalAnalise,
        Map<String, String> versaoPrompts) {

    public PropriedadesIa {
        versaoPrompts = versaoPrompts == null ? Map.of() : Map.copyOf(versaoPrompts);
    }
}
