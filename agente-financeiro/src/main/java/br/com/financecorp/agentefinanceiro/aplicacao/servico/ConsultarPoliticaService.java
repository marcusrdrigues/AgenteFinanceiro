package br.com.financecorp.agentefinanceiro.aplicacao.servico;

import br.com.financecorp.agentefinanceiro.aplicacao.contexto.MontadorContexto;
import br.com.financecorp.agentefinanceiro.aplicacao.excecao.SaidaModeloInvalidaException;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.ContextoMontado;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.Fonte;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.RespostaDocumentalRedigida;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.TrechoRotulado;
import br.com.financecorp.agentefinanceiro.aplicacao.porta.BuscaDocumentoPort;
import br.com.financecorp.agentefinanceiro.aplicacao.porta.RedatorDocumentalPort;
import br.com.financecorp.agentefinanceiro.aplicacao.validacao.ResultadoValidacao;
import br.com.financecorp.agentefinanceiro.aplicacao.validacao.ValidadorReferencias;
import java.util.List;
import java.util.Optional;

/**
 * Caminho DOCUMENTAL (US01). Busca e monta o contexto em código; sem trecho acima do limiar, a
 * resposta é a mensagem fixa e o modelo não é chamado (FR-020). Saída que não valida volta ao
 * modelo com o motivo, no máximo {@code tentativasValidacao} vezes.
 */
public final class ConsultarPoliticaService {

    public static final String MENSAGEM_SEM_FONTE =
            "Não encontrei essa informação nos documentos autorizados da FinanceCorp.";
    public static final String MENSAGEM_FALHA =
            "Não consegui elaborar uma resposta confiável com base nos documentos autorizados. "
                    + "Tente reformular a pergunta.";
    static final String AUSENCIA_SEM_FONTE = "informação não encontrada nos documentos autorizados";
    static final String AUSENCIA_FALHA = "resposta confiável com base nos documentos";

    /** Resultado do caminho documental; {@code documentosConsultados} vai para o registro de execução. */
    public record ResultadoPolitica(
            String texto, List<Fonte> fontes, List<String> ausencias, List<String> documentosConsultados,
            boolean semFonte) {
    }

    private final BuscaDocumentoPort busca;
    private final RedatorDocumentalPort redator;
    private final MontadorContexto montador;
    private final ValidadorReferencias validador;
    private final int tentativasValidacao;

    public ConsultarPoliticaService(BuscaDocumentoPort busca, RedatorDocumentalPort redator,
            MontadorContexto montador, ValidadorReferencias validador, int tentativasValidacao) {
        this.busca = busca;
        this.redator = redator;
        this.montador = montador;
        this.validador = validador;
        this.tentativasValidacao = tentativasValidacao;
    }

    public ResultadoPolitica consultar(String pergunta) {
        ContextoMontado contexto = montador.montar(busca.buscar(pergunta));
        List<String> consultados = contexto.trechos().stream()
                .map(TrechoRotulado::trecho)
                .map(t -> t.titulo() + "@" + t.versao())
                .distinct()
                .toList();
        if (contexto.estaVazio()) {
            return semFonte(consultados);
        }

        Optional<String> correcao = Optional.empty();
        for (int tentativa = 0; tentativa <= tentativasValidacao; tentativa++) {
            RespostaDocumentalRedigida redigida;
            try {
                redigida = redator.redigir(pergunta, contexto, correcao);
            } catch (SaidaModeloInvalidaException invalida) {
                correcao = Optional.of("A resposta anterior não seguiu o formato pedido: " + invalida.getMessage());
                continue;
            }
            ResultadoValidacao resultado = validador.validar(redigida, contexto);
            if (!resultado.aprovado()) {
                correcao = resultado.motivoReprovacao();
                continue;
            }
            if (!redigida.conhecimentoSuficiente()) {
                return semFonte(consultados);
            }
            return new ResultadoPolitica(redigida.texto(), validador.fontes(redigida, contexto), List.of(),
                    consultados, false);
        }
        return new ResultadoPolitica(MENSAGEM_FALHA, List.of(), List.of(AUSENCIA_FALHA), consultados, false);
    }

    private static ResultadoPolitica semFonte(List<String> consultados) {
        return new ResultadoPolitica(MENSAGEM_SEM_FONTE, List.of(), List.of(AUSENCIA_SEM_FONTE), consultados, true);
    }
}
