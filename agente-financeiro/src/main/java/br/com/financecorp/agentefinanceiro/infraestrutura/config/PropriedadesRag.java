package br.com.financecorp.agentefinanceiro.infraestrutura.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Busca e divisão de documentos; top-k e limiar são calibrados pelo eval de recuperação (research D6). */
@Validated
@ConfigurationProperties("financeiro.rag")
public record PropriedadesRag(
        @Min(1) int topK,
        @DecimalMin("-1.0") @DecimalMax("1.0") double limiarSimilaridade,
        @Min(100) int tamanhoMaximoTrecho,
        @Min(0) int sobreposicao,
        @Min(1) int tamanhoMinimoTrecho,
        @Min(1) int maximoTrechosPorDocumento,
        @Min(100) int tamanhoMaximoContexto,
        @Min(1) int loteGravacao) {

    /** Sobreposição igual ou maior que o trecho faria o divisor andar para trás. */
    @AssertTrue(message = "sobreposicao deve ser menor que tamanho-maximo-trecho")
    public boolean isSobreposicaoMenorQueTrecho() {
        return sobreposicao < tamanhoMaximoTrecho;
    }
}
