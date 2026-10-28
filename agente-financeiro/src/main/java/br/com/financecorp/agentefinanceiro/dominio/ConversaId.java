package br.com.financecorp.agentefinanceiro.dominio;

import br.com.financecorp.agentefinanceiro.dominio.excecao.ConversaIdInvalidoException;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Identificador de conversa: UUID versão 4 gerado pelo cliente (FR-002). Sem autenticação, é a
 * única barreira entre conversas; por isso só a forma aleatória e canônica é aceita.
 */
public record ConversaId(UUID valor) {

    // UUID.fromString aceita formas não canônicas ("1-1-1-1-1"); a verificação de formato vem antes.
    private static final Pattern UUID_V4_CANONICO = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$");

    public ConversaId {
        Objects.requireNonNull(valor, "valor");
        if (valor.version() != 4) {
            throw new ConversaIdInvalidoException();
        }
    }

    public static ConversaId de(String texto) {
        if (texto == null || !UUID_V4_CANONICO.matcher(texto).matches()) {
            throw new ConversaIdInvalidoException();
        }
        return new ConversaId(UUID.fromString(texto));
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
