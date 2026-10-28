package br.com.financecorp.agentefinanceiro.aplicacao.contexto;

/**
 * Neutraliza texto que vai para dentro de um bloco do prompt: remove caracteres de controle (menos
 * quebra de linha e tabulação) e troca {@code < >} por {@code ‹ ›}, para que nenhum conteúdo feche um
 * delimitador ({@code </trechos>}, {@code </pergunta>}) e abra outro.
 */
public final class Neutralizador {

    private Neutralizador() {
    }

    public static String neutralizar(String valor) {
        if (valor == null) {
            return "";
        }
        StringBuilder limpo = new StringBuilder(valor.length());
        valor.codePoints().forEach(c -> {
            if (c == '<') {
                limpo.append('‹');
            } else if (c == '>') {
                limpo.append('›');
            } else if (!Character.isISOControl(c) || c == '\n' || c == '\t') {
                limpo.appendCodePoint(c);
            }
        });
        return limpo.toString();
    }
}
