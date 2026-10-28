package br.com.financecorp.agentefinanceiro.infraestrutura.ingestao;

import br.com.financecorp.agentefinanceiro.aplicacao.modelo.DocumentoParaIngestao;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.ResultadoIngestao;
import br.com.financecorp.agentefinanceiro.aplicacao.servico.IngerirDocumentoService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Carga administrativa do conjunto curado (spec: "processo administrativo prévio"). Só roda no
 * perfil {@code ingestao}, com o papel agente_ingestao. Não há endpoint de upload nesta versão.
 */
@Component
@Profile("ingestao")
public final class IngestaoDocumentosRunner implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(IngestaoDocumentosRunner.class);
    /** Limite de tamanho por arquivo; acima disso o arquivo é recusado sem ser lido. */
    private static final long TAMANHO_MAXIMO_BYTES = 512 * 1024;

    private final IngerirDocumentoService ingestao;
    private final Path pasta;

    public IngestaoDocumentosRunner(IngerirDocumentoService ingestao,
            @Value("${financeiro.ingestao.pasta:documentos}") Path pasta) {
        this.ingestao = ingestao;
        this.pasta = pasta;
    }

    @Override
    public void run(ApplicationArguments argumentos) {
        executar();
    }

    public void executar() {
        String cadastradoPor = "ingestao:" + System.getProperty("user.name", "desconhecido");
        Set<String> presentes = new HashSet<>();
        for (Path arquivo : arquivosMarkdown()) {
            String nome = arquivo.getFileName().toString();
            presentes.add(nome);
            try {
                if (Files.size(arquivo) > TAMANHO_MAXIMO_BYTES) {
                    LOG.warn("Arquivo {} recusado: acima de {} bytes", nome, TAMANHO_MAXIMO_BYTES);
                    continue;
                }
                DocumentoParaIngestao documento =
                        LeitorDocumentoMarkdown.ler(nome, Files.readString(arquivo, StandardCharsets.UTF_8));
                ResultadoIngestao resultado = ingestao.ingerir(documento, cadastradoPor);
                LOG.info("Ingestão: {} {} -> {}", documento.titulo(), documento.versao(), resultado);
            } catch (IOException | RuntimeException falha) {
                LOG.error("Ingestão de {} falhou: {}", nome, falha.getMessage());
            }
        }
        int removidos = ingestao.sincronizar(presentes);
        LOG.info("Ingestão concluída: {} arquivo(s) lidos, {} documento(s) removido(s)", presentes.size(), removidos);
    }

    private List<Path> arquivosMarkdown() {
        if (!Files.isDirectory(pasta)) {
            throw new IllegalStateException("Pasta de documentos não encontrada: " + pasta.toAbsolutePath());
        }
        try (Stream<Path> arquivos = Files.list(pasta)) {
            return arquivos.filter(p -> p.getFileName().toString().endsWith(".md")).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
