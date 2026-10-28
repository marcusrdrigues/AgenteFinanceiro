package br.com.financecorp.agentefinanceiro.aplicacao.validacao;

import br.com.financecorp.agentefinanceiro.aplicacao.modelo.ContextoMontado;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.Fonte;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.RespostaDocumentalRedigida;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.TrechoDocumento;
import br.com.financecorp.agentefinanceiro.dominio.Dinheiro;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Confere a resposta documental contra os trechos (FR-019, FR-040): toda referência existe,
 * resposta com conhecimento cita ao menos um trecho e nenhum valor em reais aparece sem estar num
 * trecho referenciado. As fontes saem dos trechos, nunca do texto do modelo.
 */
public final class ValidadorReferencias {

    public ResultadoValidacao validar(RespostaDocumentalRedigida resposta, ContextoMontado contexto) {
        for (String ref : resposta.referencias()) {
            if (contexto.porRotulo(ref).isEmpty()) {
                return ResultadoValidacao.reprovar("A referência " + ref + " não existe entre os trechos fornecidos.");
            }
        }
        if (resposta.conhecimentoSuficiente() && resposta.referencias().isEmpty()) {
            return ResultadoValidacao.reprovar(
                    "Resposta com conhecimentoSuficiente=true precisa citar ao menos um trecho em referencias.");
        }
        Set<Dinheiro> valoresDosTrechos = new HashSet<>();
        trechosReferenciados(resposta, contexto)
                .forEach(t -> valoresDosTrechos.addAll(ExtratorValoresMonetarios.extrair(t.texto())));
        for (Dinheiro valor : ExtratorValoresMonetarios.extrair(resposta.texto())) {
            if (!valoresDosTrechos.contains(valor)) {
                return ResultadoValidacao.reprovar("O valor " + valor.formatarBrl()
                        + " não aparece nos trechos citados. Use apenas valores presentes nos trechos.");
            }
        }
        return ResultadoValidacao.aprovar();
    }

    /** Fontes dos trechos referenciados, sem repetição, em ordem de similaridade. */
    public List<Fonte> fontes(RespostaDocumentalRedigida resposta, ContextoMontado contexto) {
        Set<Fonte> fontes = new LinkedHashSet<>();
        trechosReferenciados(resposta, contexto).stream()
                .sorted(Comparator.comparing(TrechoDocumento::similaridade,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .forEach(t -> fontes.add(Fonte.documento(t.titulo(), t.versao(), t.secao())));
        return List.copyOf(fontes);
    }

    private static List<TrechoDocumento> trechosReferenciados(
            RespostaDocumentalRedigida resposta, ContextoMontado contexto) {
        return resposta.referencias().stream()
                .distinct()
                .map(contexto::porRotulo)
                .flatMap(Optional::stream)
                .toList();
    }
}
