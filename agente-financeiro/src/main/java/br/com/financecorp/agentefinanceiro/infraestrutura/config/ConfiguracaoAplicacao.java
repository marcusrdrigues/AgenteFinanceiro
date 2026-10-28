package br.com.financecorp.agentefinanceiro.infraestrutura.config;

import br.com.financecorp.agentefinanceiro.aplicacao.contexto.DivisorTexto;
import br.com.financecorp.agentefinanceiro.aplicacao.contexto.MontadorContexto;
import br.com.financecorp.agentefinanceiro.aplicacao.porta.BuscaDocumentoPort;
import br.com.financecorp.agentefinanceiro.aplicacao.porta.ClassificadorPerguntaPort;
import br.com.financecorp.agentefinanceiro.aplicacao.porta.GravacaoTrechosPort;
import br.com.financecorp.agentefinanceiro.aplicacao.porta.RedatorDocumentalPort;
import br.com.financecorp.agentefinanceiro.aplicacao.porta.RegistroExecucaoPort;
import br.com.financecorp.agentefinanceiro.aplicacao.porta.RepositorioDocumentoPort;
import br.com.financecorp.agentefinanceiro.aplicacao.servico.ConsultarPoliticaService;
import br.com.financecorp.agentefinanceiro.aplicacao.servico.IngerirDocumentoService;
import br.com.financecorp.agentefinanceiro.aplicacao.servico.PublicacaoDocumento;
import br.com.financecorp.agentefinanceiro.aplicacao.servico.ResponderPerguntaService;
import br.com.financecorp.agentefinanceiro.aplicacao.validacao.DetectorInjecao;
import br.com.financecorp.agentefinanceiro.aplicacao.validacao.ValidadorClassificacao;
import br.com.financecorp.agentefinanceiro.aplicacao.validacao.ValidadorReferencias;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Monta os casos de uso com as portas e os parâmetros. A camada de aplicação não tem anotação de
 * componente nem conhece as classes de propriedades: a fiação fica aqui.
 */
@Configuration
public class ConfiguracaoAplicacao {

    @Bean
    Clock relogio() {
        return Clock.systemUTC();
    }

    @Bean
    MontadorContexto montadorContexto(PropriedadesRag rag) {
        return new MontadorContexto(rag.limiarSimilaridade(), rag.tamanhoMaximoTrecho(), rag.tamanhoMaximoContexto());
    }

    @Bean
    ConsultarPoliticaService consultarPoliticaService(BuscaDocumentoPort busca, RedatorDocumentalPort redator,
            MontadorContexto montador, PropriedadesIa ia) {
        return new ConsultarPoliticaService(busca, redator, montador, new ValidadorReferencias(),
                ia.tentativasValidacao());
    }

    @Bean
    ResponderPerguntaService responderPerguntaService(ClassificadorPerguntaPort classificador,
            ConsultarPoliticaService politicas, RegistroExecucaoPort registro, Clock relogio, PropriedadesIa ia,
            PropriedadesLimites limites) {
        return new ResponderPerguntaService(classificador,
                new ValidadorClassificacao(limites.tamanhoMaximoPergunta()), politicas, registro, relogio,
                new ResponderPerguntaService.Parametros(ia.tentativasValidacao(), ia.tempoTotal(),
                        limites.tamanhoMaximoPergunta(), ia.versaoPrompts()));
    }

    @Bean
    PublicacaoDocumento publicacaoDocumento(RepositorioDocumentoPort documentos, GravacaoTrechosPort trechos) {
        return new PublicacaoDocumento(documentos, trechos);
    }

    @Bean
    IngerirDocumentoService ingerirDocumentoService(RepositorioDocumentoPort documentos,
            GravacaoTrechosPort trechos, PublicacaoDocumento publicacao, PropriedadesRag rag, Clock relogio) {
        DivisorTexto divisor = new DivisorTexto(rag.tamanhoMaximoTrecho(), rag.sobreposicao(),
                rag.tamanhoMinimoTrecho(), rag.maximoTrechosPorDocumento());
        return new IngerirDocumentoService(documentos, trechos, publicacao, divisor, new DetectorInjecao(), relogio);
    }
}
