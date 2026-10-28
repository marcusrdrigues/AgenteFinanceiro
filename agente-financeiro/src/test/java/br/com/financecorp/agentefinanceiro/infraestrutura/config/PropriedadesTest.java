package br.com.financecorp.agentefinanceiro.infraestrutura.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class PropriedadesTest {

    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withUserConfiguration(Configuracao.class)
            .withPropertyValues(
                    "financeiro.rag.top-k=4",
                    "financeiro.rag.limiar-similaridade=0.55",
                    "financeiro.rag.tamanho-maximo-trecho=1200",
                    "financeiro.rag.sobreposicao=200",
                    "financeiro.rag.tamanho-minimo-trecho=100",
                    "financeiro.rag.maximo-trechos-por-documento=200",
                    "financeiro.rag.tamanho-maximo-contexto=4800",
                    "financeiro.rag.lote-gravacao=50",
                    "financeiro.ia.tentativas-validacao=2",
                    "financeiro.ia.tentativas-transitorias=2",
                    "financeiro.ia.backoff-inicial=500ms",
                    "financeiro.ia.multiplicador-backoff=2",
                    "financeiro.ia.tempo-total=25s",
                    "financeiro.ia.tempo-total-analise=30s");

    @Test
    void deveCarregarValoresValidos() {
        contexto.run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx.getBean(PropriedadesRag.class).topK()).isEqualTo(4);
            assertThat(ctx.getBean(PropriedadesIa.class).tentativasTransitorias()).isEqualTo(2);
        });
    }

    @Test
    void deveRecusarTopKZero() {
        contexto.withPropertyValues("financeiro.rag.top-k=0")
                .run(ctx -> assertThat(ctx).hasFailed());
    }

    @Test
    void deveRecusarSobreposicaoMaiorQueOTrecho() {
        contexto.withPropertyValues("financeiro.rag.sobreposicao=1200")
                .run(ctx -> assertThat(ctx).hasFailed());
    }

    @Test
    void deveRecusarMaisDeDuasTentativasDeValidacao() {
        contexto.withPropertyValues("financeiro.ia.tentativas-validacao=3")
                .run(ctx -> assertThat(ctx).hasFailed());
    }

    @Configuration
    @EnableConfigurationProperties({PropriedadesRag.class, PropriedadesIa.class})
    static class Configuracao {
    }
}
