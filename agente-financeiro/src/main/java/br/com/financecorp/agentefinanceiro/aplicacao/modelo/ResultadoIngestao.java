package br.com.financecorp.agentefinanceiro.aplicacao.modelo;

/** Desfecho da carga de um arquivo; vai para o log só com título, versão e contagem. */
public enum ResultadoIngestao {
    PUBLICADO,
    NAO_AUTORIZADO,
    IGNORADO_DUPLICADO,
    EM_REVISAO,
    ERRO_PROCESSAMENTO
}
