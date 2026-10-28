package br.com.financecorp.agentefinanceiro.infraestrutura.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Limites dos critérios objetivos de risco (FR-022), documentados em docs/criterios-risco.md. */
@Validated
@ConfigurationProperties("financeiro.risco")
public record PropriedadesRisco(
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal despesaSobreReceitaMaxima,
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal quedaReceitaMinima) {
}
