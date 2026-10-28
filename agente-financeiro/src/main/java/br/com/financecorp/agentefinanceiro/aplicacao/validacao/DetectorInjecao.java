package br.com.financecorp.agentefinanceiro.aplicacao.validacao;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Primeira camada contra conteúdo envenenado na ingestão: padrões comuns de injeção mandam o
 * documento para revisão. É uma camada que falha; a defesa que segura é o agente ter poucos
 * privilégios e tratar todo trecho como dado.
 */
public final class DetectorInjecao {

    private static final List<Pattern> PADROES = List.of(
            Pattern.compile("ignore (as|todas as|quaisquer) (instrucoes|regras|orientacoes)"),
            Pattern.compile("ignore (all |the )?(previous|prior) (instructions|rules)"),
            Pattern.compile("desconsidere (as )?(instrucoes|regras) anteriores"),
            Pattern.compile("voce agora e"),
            Pattern.compile("you are now"),
            Pattern.compile("revele (o|seu) prompt"),
            Pattern.compile("system prompt"),
            Pattern.compile("(?m)^\\s*(system|assistant|developer)\\s*:"),
            Pattern.compile("</?(trechos|trecho|pergunta|historico|numeros)>"));

    public boolean contemPadraoDeInjecao(String texto) {
        String normalizado = normalizar(texto);
        return PADROES.stream().anyMatch(p -> p.matcher(normalizado).find());
    }

    private static String normalizar(String texto) {
        String semAcento = Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcento.toLowerCase(Locale.ROOT);
    }
}
