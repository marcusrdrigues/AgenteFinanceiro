package br.com.financecorp.agentefinanceiro.aplicacao.contexto;

import br.com.financecorp.agentefinanceiro.aplicacao.excecao.DocumentoGrandeDemaisException;
import java.util.ArrayList;
import java.util.List;

/**
 * Divide texto em trechos com sobreposição, medindo em caracteres. Escrito à mão porque o
 * TokenTextSplitter do Spring AI 2.0 não tem sobreposição; emendar sobreposição depois misturaria
 * duas regras de corte e quebraria o determinismo do id por ordem.
 */
public final class DivisorTexto {

    private final int tamanhoMaximo;
    private final int sobreposicao;
    private final int tamanhoMinimo;
    private final int maximoTrechos;

    public DivisorTexto(int tamanhoMaximo, int sobreposicao, int tamanhoMinimo, int maximoTrechos) {
        if (sobreposicao >= tamanhoMaximo) {
            throw new IllegalArgumentException("sobreposicao deve ser menor que tamanhoMaximo");
        }
        this.tamanhoMaximo = tamanhoMaximo;
        this.sobreposicao = sobreposicao;
        this.tamanhoMinimo = tamanhoMinimo;
        this.maximoTrechos = maximoTrechos;
    }

    public List<String> dividir(String bruto) {
        String texto = bruto == null ? "" : bruto.strip();
        if (texto.isEmpty()) {
            return List.of();
        }
        if (texto.length() <= tamanhoMaximo) {
            return List.of(texto);
        }
        List<String> trechos = new ArrayList<>();
        int inicio = 0;
        while (inicio < texto.length()) {
            int fimJanela = Math.min(inicio + tamanhoMaximo, texto.length());
            int corte = fimJanela == texto.length() ? fimJanela : melhorCorte(texto, inicio, fimJanela);
            String trecho = texto.substring(inicio, corte).strip();
            if (!trecho.isEmpty()) {
                trechos.add(trecho);
                if (trechos.size() > maximoTrechos) {
                    throw new DocumentoGrandeDemaisException(maximoTrechos);
                }
            }
            if (corte >= texto.length()) {
                break;
            }
            inicio = proximoInicio(texto, inicio, corte);
        }
        descartarUltimoPequeno(trechos);
        return List.copyOf(trechos);
    }

    /** Procura de trás para frente: fim de parágrafo, fim de frase, quebra de linha, espaço. */
    private int melhorCorte(String texto, int inicio, int fimJanela) {
        // O corte precisa passar da sobreposição; senão o próximo início não avança.
        int minimo = inicio + sobreposicao + 1;
        int paragrafo = texto.lastIndexOf("\n\n", fimJanela - 2);
        if (paragrafo >= minimo) {
            return paragrafo;
        }
        for (int i = fimJanela - 1; i >= minimo; i--) {
            char c = texto.charAt(i - 1);
            if ((c == '.' || c == '!' || c == '?') && Character.isWhitespace(texto.charAt(i))) {
                return i;
            }
        }
        int linha = texto.lastIndexOf('\n', fimJanela - 1);
        if (linha >= minimo) {
            return linha;
        }
        int espaco = texto.lastIndexOf(' ', fimJanela - 1);
        if (espaco >= minimo) {
            return espaco;
        }
        return fimJanela;
    }

    /** Volta a sobreposição e avança até o início de uma palavra, sempre depois do início atual. */
    private int proximoInicio(String texto, int inicio, int corte) {
        int proximo = Math.max(corte - sobreposicao, inicio + 1);
        while (proximo < corte && !Character.isWhitespace(texto.charAt(proximo - 1))) {
            proximo++;
        }
        while (proximo < corte && Character.isWhitespace(texto.charAt(proximo))) {
            proximo++;
        }
        return proximo >= corte ? corte : proximo;
    }

    private void descartarUltimoPequeno(List<String> trechos) {
        if (trechos.size() > 1 && trechos.getLast().length() < tamanhoMinimo) {
            trechos.removeLast();
        }
    }
}
