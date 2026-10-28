package br.com.financecorp.agentefinanceiro.aplicacao.modelo;

import br.com.financecorp.agentefinanceiro.dominio.ConversaId;
import br.com.financecorp.agentefinanceiro.dominio.Periodo;
import br.com.financecorp.agentefinanceiro.dominio.TipoResposta;
import java.util.List;
import java.util.Objects;

/**
 * Resultado do caso de uso, base do DTO HTTP (FR-042). Os campos de análise e de avaliação de risco
 * entram nos incrementos 5 e 7.
 */
public record RespostaAssistente(
        ConversaId conversaId,
        TipoResposta tipo,
        String texto,
        List<ValorMonetario> valores,
        Periodo periodo,
        List<Fonte> fontes,
        List<String> ausencias) {

    public RespostaAssistente {
        Objects.requireNonNull(conversaId, "conversaId");
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(texto, "texto");
        valores = valores == null ? List.of() : List.copyOf(valores);
        fontes = fontes == null ? List.of() : List.copyOf(fontes);
        ausencias = ausencias == null ? List.of() : List.copyOf(ausencias);
    }
}
