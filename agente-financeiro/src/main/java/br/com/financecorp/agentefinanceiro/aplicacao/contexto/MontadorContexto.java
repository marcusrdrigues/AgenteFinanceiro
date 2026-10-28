package br.com.financecorp.agentefinanceiro.aplicacao.contexto;

import br.com.financecorp.agentefinanceiro.aplicacao.modelo.ContextoMontado;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.TrechoDocumento;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.TrechoRotulado;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Monta o bloco {@code <trechos>} em código, sem framework de IA: filtra pelo limiar, ordena,
 * sanitiza, limita e rotula. O conteúdo dos documentos é dado; neutralizar os delimitadores impede
 * que um trecho feche o bloco e abra outro (FR-021).
 */
public final class MontadorContexto {

    private final double limiarSimilaridade;
    private final int tamanhoMaximoTrecho;
    private final int tamanhoMaximoContexto;

    public MontadorContexto(double limiarSimilaridade, int tamanhoMaximoTrecho, int tamanhoMaximoContexto) {
        this.limiarSimilaridade = limiarSimilaridade;
        this.tamanhoMaximoTrecho = tamanhoMaximoTrecho;
        this.tamanhoMaximoContexto = tamanhoMaximoContexto;
    }

    public ContextoMontado montar(List<TrechoDocumento> recuperados) {
        List<TrechoDocumento> ordenados = recuperados.stream()
                .filter(t -> t.similaridade() == null || t.similaridade() >= limiarSimilaridade)
                // Ordenação estável: similaridade nula vai para o fim mantendo a ordem da busca.
                .sorted(Comparator.comparing(TrechoDocumento::similaridade,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::sanitizar)
                .toList();

        List<TrechoRotulado> rotulados = new ArrayList<>();
        int total = 0;
        for (TrechoDocumento trecho : ordenados) {
            if (total + trecho.texto().length() > tamanhoMaximoContexto) {
                continue;
            }
            total += trecho.texto().length();
            rotulados.add(new TrechoRotulado("T" + (rotulados.size() + 1), trecho));
        }
        if (rotulados.isEmpty()) {
            return ContextoMontado.vazio();
        }
        return new ContextoMontado(rotulados, bloco(rotulados));
    }

    private TrechoDocumento sanitizar(TrechoDocumento t) {
        String texto = Neutralizador.neutralizar(t.texto());
        if (texto.length() > tamanhoMaximoTrecho) {
            texto = texto.substring(0, tamanhoMaximoTrecho);
        }
        return new TrechoDocumento(t.documentoId(), Neutralizador.neutralizar(t.titulo()), Neutralizador.neutralizar(t.versao()),
                Neutralizador.neutralizar(t.secao()), texto, t.similaridade());
    }

    private static String bloco(List<TrechoRotulado> rotulados) {
        StringBuilder bloco = new StringBuilder("<trechos>\n");
        for (TrechoRotulado r : rotulados) {
            TrechoDocumento t = r.trecho();
            bloco.append("<trecho ref=\"").append(r.rotulo())
                    .append("\" titulo=\"").append(atributo(t.titulo()))
                    .append("\" versao=\"").append(atributo(t.versao()))
                    .append("\" secao=\"").append(atributo(t.secao()))
                    .append("\">\n").append(t.texto()).append("\n</trecho>\n");
        }
        return bloco.append("</trechos>").toString();
    }

    private static String atributo(String valor) {
        return valor.replace("\"", "&quot;").replace("\n", " ");
    }
}
