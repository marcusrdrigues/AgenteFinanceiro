package br.com.financecorp.agentefinanceiro.aplicacao.servico;

import br.com.financecorp.agentefinanceiro.aplicacao.contexto.DivisorTexto;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.DocumentoParaIngestao;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.DocumentoRegistrado;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.ResultadoIngestao;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.SituacaoDocumento;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.TrechoParaGravar;
import br.com.financecorp.agentefinanceiro.aplicacao.porta.GravacaoTrechosPort;
import br.com.financecorp.agentefinanceiro.aplicacao.porta.RepositorioDocumentoPort;
import br.com.financecorp.agentefinanceiro.aplicacao.validacao.DetectorInjecao;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Esteira de ingestão do conjunto curado (FR-045; rag-pgvector §4): normaliza, deduplica, analisa
 * injeção, divide por seção, grava trechos invisíveis e só então publica. Falha em qualquer passo
 * de gravação não deixa trecho: o documento fica como ERRO_PROCESSAMENTO.
 */
public final class IngerirDocumentoService {

    private final RepositorioDocumentoPort documentos;
    private final GravacaoTrechosPort trechos;
    private final PublicacaoDocumento publicacao;
    private final DivisorTexto divisor;
    private final DetectorInjecao detector;
    private final Clock relogio;

    public IngerirDocumentoService(RepositorioDocumentoPort documentos, GravacaoTrechosPort trechos,
            PublicacaoDocumento publicacao, DivisorTexto divisor, DetectorInjecao detector, Clock relogio) {
        this.documentos = documentos;
        this.trechos = trechos;
        this.publicacao = publicacao;
        this.divisor = divisor;
        this.detector = detector;
        this.relogio = relogio;
    }

    public ResultadoIngestao ingerir(DocumentoParaIngestao documento, String cadastradoPor) {
        String texto = normalizar(documento.texto());
        String hash = sha256(texto);
        Optional<DocumentoRegistrado> mesmoConteudo = documentos.buscarPorHash(hash);
        if (mesmoConteudo.isPresent()) {
            // Só é duplicado o que terminou bem. Erro de processamento volta a tentar; em revisão volta a
            // ser analisado (o revisor pode ter marcado revisaoHumana: aprovada sem mudar o texto).
            SituacaoDocumento situacao = mesmoConteudo.get().situacao();
            if (situacao != SituacaoDocumento.ERRO_PROCESSAMENTO && situacao != SituacaoDocumento.EM_REVISAO) {
                return ResultadoIngestao.IGNORADO_DUPLICADO;
            }
            apagar(mesmoConteudo.get());
        }
        // Mesmo arquivo com conteúdo novo: a versão antiga dele sai inteira antes da nova entrar.
        documentos.listarTodos().stream()
                .filter(d -> d.arquivoOrigem().equals(documento.arquivoOrigem()))
                .forEach(this::apagar);

        UUID documentoId = documentos.inserir(documento, hash, trechos.modeloEmbedding(), cadastradoPor);
        if (detector.contemPadraoDeInjecao(texto) && !documento.revisaoHumanaAprovada()) {
            documentos.atualizarSituacao(documentoId, SituacaoDocumento.EM_REVISAO, 0);
            return ResultadoIngestao.EM_REVISAO;
        }
        try {
            List<TrechoParaGravar> divididos = dividir(documentoId, documento, texto, cadastradoPor);
            trechos.gravar(divididos);
            publicacao.publicar(documentoId, documento.titulo(), documento.situacaoDeclarada(), divididos.size());
            return documento.situacaoDeclarada() == SituacaoDocumento.AUTORIZADO
                    ? ResultadoIngestao.PUBLICADO
                    : ResultadoIngestao.NAO_AUTORIZADO;
        } catch (RuntimeException falha) {
            trechos.removerPorDocumento(documentoId);
            documentos.atualizarSituacao(documentoId, SituacaoDocumento.ERRO_PROCESSAMENTO, 0);
            return ResultadoIngestao.ERRO_PROCESSAMENTO;
        }
    }

    /** Apaga documento, trechos e vetores de todo arquivo que saiu do conjunto curado. Devolve quantos. */
    public int sincronizar(Set<String> arquivosPresentes) {
        List<DocumentoRegistrado> ausentes = documentos.listarTodos().stream()
                .filter(d -> !arquivosPresentes.contains(d.arquivoOrigem()))
                .toList();
        ausentes.forEach(this::apagar);
        return ausentes.size();
    }

    private void apagar(DocumentoRegistrado documento) {
        trechos.removerPorDocumento(documento.id());
        documentos.remover(documento.id());
    }

    private List<TrechoParaGravar> dividir(UUID documentoId, DocumentoParaIngestao documento, String texto,
            String cadastradoPor) {
        Instant agora = relogio.instant();
        List<TrechoParaGravar> resultado = new ArrayList<>();
        for (Secao secao : secoes(texto, documento.titulo())) {
            for (String pedaco : divisor.dividir(secao.conteudo())) {
                int ordem = resultado.size();
                // Id determinístico: a mesma carga gera os mesmos ids.
                UUID id = UUID.nameUUIDFromBytes((documentoId + ":" + ordem).getBytes(StandardCharsets.UTF_8));
                resultado.add(new TrechoParaGravar(id, documentoId, ordem, documento.titulo(), documento.versao(),
                        secao.nome(), pedaco, SituacaoDocumento.PROCESSANDO, documento.arquivoOrigem(),
                        cadastradoPor, agora));
            }
        }
        return resultado;
    }

    private record Secao(String nome, String conteudo) {
    }

    /** Seções pelos títulos {@code ## }; o que vem antes do primeiro fica na seção "Introdução". */
    private static List<Secao> secoes(String texto, String titulo) {
        List<Secao> secoes = new ArrayList<>();
        String nome = "Introdução";
        StringBuilder conteudo = new StringBuilder();
        for (String linha : texto.split("\n", -1)) {
            if (linha.startsWith("## ")) {
                adicionar(secoes, nome, conteudo);
                nome = linha.substring(3).strip();
                conteudo = new StringBuilder();
            } else if (!linha.startsWith("# ")) {
                conteudo.append(linha).append('\n');
            }
        }
        adicionar(secoes, nome, conteudo);
        return secoes;
    }

    private static void adicionar(List<Secao> secoes, String nome, StringBuilder conteudo) {
        if (!conteudo.toString().isBlank()) {
            secoes.add(new Secao(nome, conteudo.toString().strip()));
        }
    }

    static String normalizar(String texto) {
        String nfc = Normalizer.normalize(texto.replace("\r\n", "\n").replace('\r', '\n'), Normalizer.Form.NFC);
        StringBuilder limpo = new StringBuilder(nfc.length());
        nfc.codePoints()
                .filter(c -> !Character.isISOControl(c) || c == '\n' || c == '\t')
                .forEach(limpo::appendCodePoint);
        return limpo.toString().strip();
    }

    private static String sha256(String texto) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível na JVM", e);
        }
    }
}
