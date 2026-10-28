package br.com.financecorp.agentefinanceiro.infraestrutura.vetorial;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.financecorp.agentefinanceiro.BaseConhecimentoIT;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.TrechoDocumento;
import br.com.financecorp.agentefinanceiro.aplicacao.porta.BuscaDocumentoPort;
import br.com.financecorp.agentefinanceiro.infraestrutura.ingestao.IngestaoDocumentosRunner;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class BuscaDocumentoIT extends BaseConhecimentoIT {

    @Autowired
    private BuscaDocumentoPort busca;

    @Autowired
    private IngestaoDocumentosRunner runner;

    @BeforeEach
    void carregar() {
        escreverDocumento("reembolso.md", politica("Política de Reembolso", "v2", "AUTORIZADO",
                "## Hospedagem\nLimite diário de hospedagem em hotel durante viagem corporativa: R$ 450,00."));
        escreverDocumento("bonus.md", politica("Política de Bônus da Diretoria", "v1", "NAO_AUTORIZADO",
                "## Bônus\nLimite diário de hospedagem em hotel da diretoria durante viagem: sem teto."));
        runner.executar();
    }

    @Test
    void deveDevolverSoTrechosAutorizados() {
        List<TrechoDocumento> trechos = busca.buscar("Qual o limite diário de hospedagem em hotel na viagem?");

        assertThat(trechos).isNotEmpty();
        assertThat(trechos).extracting(TrechoDocumento::titulo).containsOnly("Política de Reembolso");
        assertThat(trechos.getFirst().secao()).isEqualTo("Hospedagem");
        assertThat(trechos.getFirst().similaridade()).isGreaterThan(0.55);
    }

    @Test
    void perguntaSemRelacaoDeveFicarAbaixoDoLimiar() {
        assertThat(busca.buscar("Previsão do tempo para amanhã em Lisboa")).isEmpty();
    }

    @Test
    void vetorDeOutroModeloDeEmbeddingNuncaAparece() {
        superusuario().update("UPDATE conhecimento.vector_store SET metadata = "
                + "jsonb_set(metadata::jsonb, '{modeloEmbedding}', '\"outro-modelo-v9\"')::json");

        assertThat(busca.buscar("Qual o limite diário de hospedagem em hotel na viagem?")).isEmpty();
    }

    @Test
    void saidaNaoTemVetorNemMetadadoInterno() {
        List<String> campos = Arrays.stream(TrechoDocumento.class.getRecordComponents())
                .map(RecordComponent::getName).toList();
        assertThat(campos).doesNotContain("embedding", "ordem", "situacao", "modeloEmbedding", "metadata");
    }
}
