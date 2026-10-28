package br.com.financecorp.agentefinanceiro.infraestrutura.ingestao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.financecorp.agentefinanceiro.aplicacao.modelo.DocumentoParaIngestao;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.SituacaoDocumento;
import br.com.financecorp.agentefinanceiro.aplicacao.validacao.DetectorInjecao;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** O conjunto curado de documentos/ é dado de teste e de aula: confere o que os cenários de US01 exigem dele. */
class DocumentosCuradosTest {

    private static List<DocumentoParaIngestao> documentos() throws Exception {
        try (Stream<Path> arquivos = Files.list(Path.of("documentos"))) {
            return arquivos.filter(p -> p.toString().endsWith(".md")).sorted()
                    .map(p -> {
                        try {
                            return LeitorDocumentoMarkdown.ler(p.getFileName().toString(),
                                    Files.readString(p, StandardCharsets.UTF_8));
                        } catch (Exception e) {
                            throw new IllegalStateException(e);
                        }
                    }).toList();
        }
    }

    @Test
    void todosOsDocumentosDevemTerFrontMatterValido() throws Exception {
        assertThat(documentos()).hasSize(6);
    }

    @Test
    void deveHaverUmDocumentoNaoAutorizado() throws Exception {
        assertThat(documentos()).filteredOn(d -> d.situacaoDeclarada() == SituacaoDocumento.NAO_AUTORIZADO)
                .extracting(DocumentoParaIngestao::titulo).containsExactly("Política de Bônus da Diretoria");
    }

    @Test
    void documentoComInjecaoDeveTerRevisaoHumanaAprovada() throws Exception {
        DetectorInjecao detector = new DetectorInjecao();
        assertThat(documentos()).filteredOn(d -> detector.contemPadraoDeInjecao(d.texto()))
                .singleElement()
                .satisfies(d -> {
                    assertThat(d.titulo()).isEqualTo("Política de Pagamento a Fornecedores");
                    assertThat(d.revisaoHumanaAprovada()).isTrue();
                });
    }

    @Test
    void nenhumDocumentoTrataDeInvestimentos() throws Exception {
        assertThat(documentos()).noneMatch(d -> d.texto().toLowerCase().contains("investiment"));
    }

    @Test
    void frontMatterIncompletoDeveSerRecusado() {
        assertThatThrownBy(() -> LeitorDocumentoMarkdown.ler("x.md", "---\ntitulo: X\n---\ncorpo"))
                .hasMessageContaining("versao");
        assertThatThrownBy(() -> LeitorDocumentoMarkdown.ler("x.md", "sem front matter"))
                .hasMessageContaining("front matter");
        assertThatThrownBy(() -> LeitorDocumentoMarkdown.ler("x.md",
                "---\ntitulo: X\nversao: v1\nvigenteDesde: 2025-01-01\nsituacao: PUBLICADO\n---\ncorpo"))
                .hasMessageContaining("situacao");
    }
}
