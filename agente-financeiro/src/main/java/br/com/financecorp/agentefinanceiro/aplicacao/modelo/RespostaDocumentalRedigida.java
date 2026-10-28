package br.com.financecorp.agentefinanceiro.aplicacao.modelo;

import java.util.List;

/** Redação do modelo no caminho documental, antes da validação. {@code referencias} são rótulos T1..Tn. */
public record RespostaDocumentalRedigida(String texto, boolean conhecimentoSuficiente, List<String> referencias) {

    public RespostaDocumentalRedigida {
        texto = texto == null ? "" : texto;
        referencias = referencias == null ? List.of() : List.copyOf(referencias);
    }
}
