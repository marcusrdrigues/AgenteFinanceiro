package br.com.financecorp.agentefinanceiro.interfaces.web.dto;

import br.com.financecorp.agentefinanceiro.aplicacao.modelo.Fonte;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.RespostaAssistente;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.ValorMonetario;
import br.com.financecorp.agentefinanceiro.dominio.Periodo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/**
 * Resposta da API (FR-042), espelhando contracts/api-agente-financeiro.yaml. Nunca leva prompt,
 * modelo, tokens, rótulos T1..Tn nem texto integral de trecho.
 */
@Schema(description = "Resposta do assistente")
public record RespostaPerguntaDto(
        String conversaId,
        @Schema(description = "Tipo de resposta; NAO_INTERPRETADA quando a classificação falhou") String tipo,
        @Schema(description = "Resposta em português do Brasil; valores no formato R$ 1.234,56") String texto,
        PeriodoDto periodo,
        List<ValorMonetarioDto> valores,
        List<FonteDto> fontes,
        @Schema(description = "Informações que faltaram; vazio quando nada faltou") List<String> ausencias) {

    public static RespostaPerguntaDto de(RespostaAssistente r) {
        return new RespostaPerguntaDto(r.conversaId().toString(), r.tipo().name(), r.texto(), PeriodoDto.de(r.periodo()),
                r.valores().stream().map(ValorMonetarioDto::de).toList(),
                r.fontes().stream().map(FonteDto::de).toList(), r.ausencias());
    }

    public record PeriodoDto(
            @Schema(example = "2025-01-01") LocalDate inicio,
            @Schema(example = "2025-03-31") LocalDate fim) {

        static PeriodoDto de(Periodo p) {
            return p == null ? null : new PeriodoDto(p.inicio(), p.fim());
        }
    }

    public record ValorMonetarioDto(
            @Schema(example = "receitas") String rotulo,
            @Schema(description = "Decimal exato como texto, nunca ponto flutuante", example = "150000.00",
                    pattern = "^-?\\d+\\.\\d{2}$") String valor,
            @Schema(example = "BRL") String moeda) {

        static ValorMonetarioDto de(ValorMonetario v) {
            return new ValorMonetarioDto(v.rotulo(), v.valor().paraDecimalTexto(), "BRL");
        }
    }

    public record FonteDto(
            @Schema(allowableValues = {"REGISTROS_FINANCEIROS", "DOCUMENTO", "AGENTE_ESPECIALISTA", "REGRAS_INTERNAS"})
            String tipo,
            String titulo,
            String versao,
            String secao) {

        static FonteDto de(Fonte f) {
            return new FonteDto(f.tipo().name(), f.titulo(), f.versao(), f.secao());
        }
    }
}
