package br.com.financecorp.agentefinanceiro.aplicacao.validacao;

import br.com.financecorp.agentefinanceiro.dominio.Dinheiro;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extrai valores em reais ({@code R$ 1.234,56}, {@code -R$ 450,00}, {@code R$ 1234}) de um texto.
 * Serve para validar a resposta do modelo contra as fontes, nunca para corrigi-la.
 */
public final class ExtratorValoresMonetarios {

    private static final Pattern VALOR = Pattern.compile(
            "(-\\s?)?R\\$\\s?(\\d{1,3}(?:\\.\\d{3})+|\\d+)(?:,(\\d{1,2}))?");

    private ExtratorValoresMonetarios() {
    }

    public static List<Dinheiro> extrair(String texto) {
        List<Dinheiro> valores = new ArrayList<>();
        if (texto == null) {
            return valores;
        }
        Matcher m = VALOR.matcher(texto);
        while (m.find()) {
            String inteiro = m.group(2).replace(".", "");
            String centavos = m.group(3) == null ? "00" : (m.group(3) + "0").substring(0, 2);
            BigDecimal valor = new BigDecimal(inteiro + "." + centavos);
            valores.add(Dinheiro.de(m.group(1) != null ? valor.negate() : valor));
        }
        return valores;
    }
}
