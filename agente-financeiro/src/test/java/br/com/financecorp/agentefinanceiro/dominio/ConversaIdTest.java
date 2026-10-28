package br.com.financecorp.agentefinanceiro.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.financecorp.agentefinanceiro.dominio.excecao.ConversaIdInvalidoException;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

class ConversaIdTest {

    @Test
    void deveAceitarUuidVersao4() {
        UUID v4 = UUID.randomUUID();
        assertThat(ConversaId.de(v4.toString()).valor()).isEqualTo(v4);
    }

    @Test
    void deveAceitarUuidVersao4EmMaiusculas() {
        String v4 = "3F0B8A3E-6A1C-4C7E-9A52-0C8F2D4B9E11";
        assertThat(ConversaId.de(v4).valor()).isEqualTo(UUID.fromString(v4));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "2b1c3d4e-0f1a-11ef-9a52-0c8f2d4b9e11", // versão 1
        "6fa459ea-ee8a-3ca4-894e-db77e160355e", // versão 3
        "01890a5d-ac96-774b-bcce-b302099a8057", // versão 7
        "123",
        "1-1-1-1-1",                            // aceito pelo UUID.fromString, mas não é forma canônica
        "3f0b8a3e-6a1c-4c7e-9a52-0c8f2d4b9e11 " // espaço extra
    })
    void deveRecusarIdentificadorForaDoFormato(String valor) {
        assertThatThrownBy(() -> ConversaId.de(valor)).isInstanceOf(ConversaIdInvalidoException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void deveRecusarNuloOuVazio(String valor) {
        assertThatThrownBy(() -> ConversaId.de(valor)).isInstanceOf(ConversaIdInvalidoException.class);
    }

    @Test
    void deveRecusarMensagemQueEcoaOValorRecebido() {
        assertThatThrownBy(() -> ConversaId.de("segredo-do-cliente"))
                .hasMessageNotContaining("segredo-do-cliente");
    }
}
