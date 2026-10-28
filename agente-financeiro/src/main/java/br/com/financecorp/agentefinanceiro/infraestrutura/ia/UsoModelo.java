package br.com.financecorp.agentefinanceiro.infraestrutura.ia;

import br.com.financecorp.agentefinanceiro.aplicacao.porta.ConsumoModeloPort;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;

/** Lê o uso informado pelo provedor na resposta e soma no consumo da requisição. */
final class UsoModelo {

    private UsoModelo() {
    }

    static void registrar(ConsumoModeloPort consumo, ChatResponse resposta) {
        if (resposta == null || resposta.getMetadata() == null) {
            return;
        }
        Usage uso = resposta.getMetadata().getUsage();
        if (uso == null || uso.getPromptTokens() == null || uso.getCompletionTokens() == null) {
            return;
        }
        consumo.somar(uso.getPromptTokens(), uso.getCompletionTokens());
    }
}
