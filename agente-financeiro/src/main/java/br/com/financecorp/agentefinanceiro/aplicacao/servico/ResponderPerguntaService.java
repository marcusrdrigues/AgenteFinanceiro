package br.com.financecorp.agentefinanceiro.aplicacao.servico;

import br.com.financecorp.agentefinanceiro.aplicacao.excecao.SaidaModeloInvalidaException;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.ClassificacaoPergunta;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.Consumo;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.RegistroExecucao;
import br.com.financecorp.agentefinanceiro.aplicacao.modelo.RespostaAssistente;
import br.com.financecorp.agentefinanceiro.aplicacao.porta.ClassificadorPerguntaPort;
import br.com.financecorp.agentefinanceiro.aplicacao.porta.ConsumoModeloPort;
import br.com.financecorp.agentefinanceiro.aplicacao.porta.RegistroExecucaoPort;
import br.com.financecorp.agentefinanceiro.aplicacao.servico.ConsultarPoliticaService.ResultadoPolitica;
import br.com.financecorp.agentefinanceiro.aplicacao.validacao.ValidadorClassificacao;
import br.com.financecorp.agentefinanceiro.aplicacao.validacao.ValidadorClassificacao.Resultado;
import br.com.financecorp.agentefinanceiro.dominio.ConversaId;
import br.com.financecorp.agentefinanceiro.dominio.TipoResposta;
import br.com.financecorp.agentefinanceiro.dominio.excecao.ExcecaoDominio;
import br.com.financecorp.agentefinanceiro.dominio.excecao.ModeloIndisponivelException;
import br.com.financecorp.agentefinanceiro.dominio.excecao.PerguntaInvalidaException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Orquestra uma pergunta: valida, classifica (lista fechada) e segue o caminho fixo do tipo
 * (FR-039, FR-040). Workflow, não agente: o modelo não escolhe o próximo passo.
 */
public final class ResponderPerguntaService {

    public static final String MENSAGEM_FORA_DO_DOMINIO = "Só posso responder a perguntas sobre as finanças da "
            + "FinanceCorp: receitas, despesas, saldo e políticas financeiras internas.";
    public static final String MENSAGEM_NAO_INTERPRETADA =
            "Não consegui interpretar a pergunta. Pode reformulá-la com mais detalhes?";
    public static final String MENSAGEM_NAO_DISPONIVEL = "Esta funcionalidade ainda não está disponível nesta etapa.";
    static final String AUSENCIA_NAO_DISPONIVEL = "funcionalidade ainda não disponível nesta etapa";
    static final String AUSENCIA_NAO_INTERPRETADA = "pergunta não interpretada";

    /** Parâmetros vindos da configuração; a aplicação não conhece as classes de propriedades. */
    public record Parametros(int tentativasValidacao, Duration tempoTotal, int tamanhoMaximoPergunta,
            Map<String, String> versoesPrompt) {
    }

    private final ClassificadorPerguntaPort classificador;
    private final ValidadorClassificacao validadorClassificacao;
    private final ConsultarPoliticaService politicas;
    private final RegistroExecucaoPort registro;
    private final ConsumoModeloPort consumo;
    private final Clock relogio;
    private final Parametros parametros;

    public ResponderPerguntaService(ClassificadorPerguntaPort classificador,
            ValidadorClassificacao validadorClassificacao, ConsultarPoliticaService politicas,
            RegistroExecucaoPort registro, ConsumoModeloPort consumo, Clock relogio, Parametros parametros) {
        this.classificador = classificador;
        this.validadorClassificacao = validadorClassificacao;
        this.politicas = politicas;
        this.registro = registro;
        this.consumo = consumo;
        this.relogio = relogio;
        this.parametros = parametros;
    }

    public RespostaAssistente responder(String conversaIdTexto, String pergunta, String traceId) {
        Instant inicio = relogio.instant();
        Execucao execucao = new Execucao();
        consumo.coletarEZerar(); // descarta sobra de uma requisição anterior que terminou fora do fluxo normal
        try {
            ConversaId conversaId = ConversaId.de(conversaIdTexto);
            execucao.conversaId = conversaId.valor();
            validarPergunta(pergunta);
            RespostaAssistente resposta = rotear(conversaId, pergunta, inicio, execucao);
            execucao.tipo = resposta.tipo();
            return resposta;
        } catch (ExcecaoDominio erro) {
            execucao.resultado = "erro";
            execucao.codigoErro = erro.codigo();
            throw erro;
        } catch (RuntimeException erro) {
            execucao.resultado = "erro";
            execucao.codigoErro = "erro-interno";
            throw erro;
        } finally {
            Consumo usado = consumo.coletarEZerar();
            registro.registrar(new RegistroExecucao(traceId, execucao.conversaId, execucao.tipo,
                    Duration.between(inicio, relogio.instant()).toMillis(), usado.tokensEntrada(), usado.tokensSaida(),
                    parametros.versoesPrompt(), execucao.documentos, List.of(), null, null,
                    execucao.resultado, execucao.codigoErro));
        }
    }

    private RespostaAssistente rotear(ConversaId conversaId, String pergunta, Instant inicio, Execucao execucao) {
        Optional<ClassificacaoPergunta> classificacao = classificar(pergunta);
        if (classificacao.isEmpty()) {
            execucao.resultado = "nao-interpretada";
            return new RespostaAssistente(conversaId, TipoResposta.NAO_INTERPRETADA, MENSAGEM_NAO_INTERPRETADA,
                    List.of(), null, List.of(), List.of(AUSENCIA_NAO_INTERPRETADA));
        }
        verificarPrazo(inicio);
        ClassificacaoPergunta c = classificacao.get();
        TipoResposta tipo = TipoResposta.de(c.tipo());
        return switch (c.tipo()) {
            case DOCUMENTAL -> {
                ResultadoPolitica r = politicas.consultar(c.perguntaAutonoma());
                execucao.documentos = r.documentosConsultados();
                execucao.resultado = r.semFonte() ? "sem-fonte" : (r.ausencias().isEmpty() ? "respondida" : "falha-validacao");
                yield new RespostaAssistente(conversaId, tipo, r.texto(), List.of(), null, r.fontes(), r.ausencias());
            }
            case FORA_DO_DOMINIO -> {
                execucao.resultado = "fora-do-dominio";
                yield new RespostaAssistente(conversaId, tipo, MENSAGEM_FORA_DO_DOMINIO, List.of(), null, List.of(),
                        List.of());
            }
            default -> {
                execucao.resultado = "nao-disponivel";
                yield new RespostaAssistente(conversaId, tipo, MENSAGEM_NAO_DISPONIVEL, List.of(), null, List.of(),
                        List.of(AUSENCIA_NAO_DISPONIVEL));
            }
        };
    }

    private Optional<ClassificacaoPergunta> classificar(String pergunta) {
        Optional<String> correcao = Optional.empty();
        for (int tentativa = 0; tentativa <= parametros.tentativasValidacao(); tentativa++) {
            Resultado resultado;
            try {
                // O histórico entra no incremento 3 (memória).
                resultado = validadorClassificacao.validar(classificador.classificar(pergunta, List.of(), correcao));
            } catch (SaidaModeloInvalidaException invalida) {
                correcao = Optional.of("A resposta anterior não seguiu o formato pedido: " + invalida.getMessage());
                continue;
            }
            if (resultado instanceof Resultado.Valida valida) {
                return Optional.of(valida.classificacao());
            }
            correcao = Optional.of(((Resultado.Invalida) resultado).motivo());
        }
        return Optional.empty();
    }

    private void validarPergunta(String pergunta) {
        if (pergunta == null || pergunta.isBlank()) {
            throw new PerguntaInvalidaException(PerguntaInvalidaException.Motivo.VAZIA);
        }
        if (pergunta.length() > parametros.tamanhoMaximoPergunta()) {
            throw new PerguntaInvalidaException(PerguntaInvalidaException.Motivo.MUITO_LONGA);
        }
    }

    /** Orçamento de tempo da requisição: estourado, as etapas restantes não rodam. */
    private void verificarPrazo(Instant inicio) {
        if (Duration.between(inicio, relogio.instant()).compareTo(parametros.tempoTotal()) > 0) {
            throw new ModeloIndisponivelException();
        }
    }

    /** Metadados acumulados ao longo da requisição para o registro de execução. */
    private static final class Execucao {
        UUID conversaId;
        TipoResposta tipo;
        List<String> documentos = List.of();
        String resultado = "respondida";
        String codigoErro;
    }
}
