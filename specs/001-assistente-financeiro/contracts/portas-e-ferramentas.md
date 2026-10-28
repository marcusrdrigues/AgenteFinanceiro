# Contratos internos: portas, ferramentas e prompts

**Plano**: [../plan.md](../plan.md) | **Modelo de dados**: [../data-model.md](../data-model.md)

As portas são interfaces declaradas em `aplicacao.porta`. A aplicação depende só delas; os
adaptadores ficam em `infraestrutura`. Assinaturas abaixo descrevem o contrato; o código final pode
ajustar nomes sem mudar o comportamento.

## 1. agente-financeiro: portas de saída

| Porta | Método | Contrato | Adaptador |
|---|---|---|---|
| `RepositorioLancamentoPort` | `ResumoFinanceiro resumir(Periodo p, Optional<Categoria> c)` | Somente leitura. Período sem lançamentos → `temDados=false`, valores zero, `cobertura=null`. Banco fora → `FonteDadosIndisponivelException`. | `JdbcRepositorioLancamentoAdapter` (`JdbcClient`, SQL com parâmetro vinculado, papel `agente_leitura`) |
| `BuscaDocumentoPort` | `List<TrechoDocumento> buscar(String pergunta)` | Só trechos `AUTORIZADO`, do modelo de embedding atual, acima do limiar, no máximo `k`; filtro **dentro** da busca vetorial. Lista vazia é resultado válido. | `PgVectorBuscaDocumentoAdapter` |
| `ClassificadorPerguntaPort` | `ClassificacaoPergunta classificar(String pergunta, List<Turno> historico)` | Saída estruturada validada (tipo da lista fechada; período válido ou nulo). Inválida → refaz até 2 vezes → `PerguntaNaoInterpretadaException`. | `OpenAiClassificadorAdapter` (`ChatClient` **sem** ferramentas) |
| `RespondedorNumericoPort` | `RespostaRedigida responder(String perguntaAutonoma, RegistroConsultas registro)` | Modelo pode chamar **apenas** as ferramentas numéricas de leitura; cada chamada é anotada em `registro`. | `OpenAiRespondedorNumericoAdapter` (`ChatClient` com `FerramentasFinanceiras`) |
| `RedatorDocumentalPort` | `RespostaDocumentalRedigida redigir(String pergunta, ContextoMontado ctx)` | Só é chamado com `ctx` não vazio. Devolve texto + referências `T1..Tn`. | `OpenAiRedatorDocumentalAdapter` (sem ferramentas; converte `RespostaDocumentalModelo` de `infraestrutura.ia`) |
| `RedatorAnalisePort` | `AnaliseRedigida redigir(ResumoFinanceiro r, AvaliacaoRiscoPorRegras risco, ContextoMontado politicas)` | Redige só `resumo` e `recomendacoes`; números ficam com o código. | `OpenAiRedatorAnaliseAdapter` (sem ferramentas; converte `AnaliseModelo` de `infraestrutura.ia`) |
| `RegistroExecucaoPort` | `void registrar(RegistroExecucao r)` | Uma linha JSON por requisição no logger `execucao`; só metadados (FR-036 a FR-038). | `Slf4jRegistroExecucaoAdapter` (arquivo com rotação diária, 90 dias) |

Todos os tipos de parâmetro e retorno das portas ficam em `aplicacao.modelo` ou `dominio`; records
de saída do modelo (`RespostaDocumentalModelo`, `AnaliseModelo`) ficam no adaptador e nunca
atravessam a porta (garantido pelo teste de arquitetura).
| `MemoriaConversaPort` | `List<Turno> recentes(ConversaId id)`; `void registrar(ConversaId id, String pergunta, String resposta)` | No máximo 20 mensagens; isolada por `conversaId`. | `SpringAiMemoriaConversaAdapter` (`ChatMemory` + repositório MongoDB) |
| `AgenteRiscoPort` | `ParecerEspecialista avaliar(IndicadoresParaRisco i)` | Tempo limite 5 s/tentativa, 1 nova tentativa só em erro transitório, disjuntor 5 falhas / 30 s, cabeçalho `Authorization: Bearer` (FR-044); 401 sem nova tentativa. Falha → `EspecialistaIndisponivelException`. | `A2aAgenteRiscoAdapter` |

Novas tentativas em erro transitório do provedor de modelo (429, 5xx, timeout): no máximo
`financeiro.ia.tentativas-transitorias=2`, com backoff exponencial (500 ms, multiplicador 2) e
variação aleatória, aplicadas num único ponto (`ExecutorChamadaModelo` em `infraestrutura.ia`);
o cliente OpenAI fica com `max-retries=0`.

Limite de requisições (FR-012): `FiltroLimiteRequisicoes` em `interfaces.web`, com Resilience4j
`RateLimiter`; não depende de `infraestrutura`.

Exceções de domínio tratadas na borda (`ErroHandler`, ProblemDetail): `ConversaIdInvalidoException`
(400), `PerguntaInvalidaException` (400/413), `LimiteRequisicoesException` (429),
`ModeloIndisponivelException` (503), `FonteDadosIndisponivelException` (503). Casos funcionais
("sem dados", "fora do domínio", "não interpretada", "especialista indisponível") **não** são erro
HTTP: viram resposta 200 com `ausencias` preenchido.

## 2. Ferramentas (`@Tool`) do agente-financeiro

Classe `FerramentasFinanceiras` em `infraestrutura.ia.ferramentas`. Delegam para
`ConsultarResumoFinanceiroService`; não acessam o banco diretamente.

| Ferramenta | Parâmetros | Retorno | Validação em código |
|---|---|---|---|
| `consultarResumoPeriodo` | `inicio` (AAAA-MM-DD), `fim` (AAAA-MM-DD) | `ResumoFerramenta` (receitas, despesas, resultado como texto decimal; período; `temDados`; cobertura) | Datas válidas, `inicio <= fim`, máx. 366 dias. Inválido → resultado de erro descritivo ao modelo, sem exceção. |
| `consultarTotalCategoria` | `inicio`, `fim`, `categoria` (enum) | `TotalCategoriaFerramenta` | Mesmas regras + categoria da lista fechada. |
| `listarCategorias` | — | lista de categorias | — |

Regras:

- Todas são **somente leitura**; não existe ferramenta de escrita (FR-009, constituição VII).
- Ferramentas só são registradas no `ChatClient` do caminho `NUMERICA`/`MISTA`. Classificador,
  redator documental e redator de análise não têm ferramentas.
- Limite: no máximo 4 chamadas de ferramenta por requisição; excedeu → falha controlada.
- Cada chamada é registrada no `RegistroConsultas` da requisição (passado às ferramentas pelo
  contexto de ferramenta do Spring AI, ver research.md), e é dele, não do texto do modelo, que
  saem os campos `valores`, `periodo` e `fontes` da resposta.

## 3. Verificação de valores (FR-007)

`VerificadorValoresResposta` (aplicação, sem Spring AI):

1. Extrai do texto redigido todo valor no formato monetário brasileiro (`R$ 1.234,56`, `-R$ ...`).
2. Converte para `Dinheiro` e confere se pertence ao conjunto de valores do `RegistroConsultas`
   (receitas, despesas, resultado, totais por categoria) ou, em `MISTA`/`DOCUMENTAL`, aparece
   literalmente num trecho citado.
3. Qualquer valor fora do conjunto → resposta reprovada; nova redação com o motivo, no máximo 2
   vezes; depois, resposta fixa de falha. A extração serve para **validar**, nunca para corrigir.

## 4. Prompts versionados

Em `agente-financeiro/src/main/resources/prompts/`:

| Arquivo | Uso |
|---|---|
| `classificar.system.v1.st` / `classificar.user.v1.st` | Classificação + reescrita autônoma + período |
| `responder-numerico.system.v1.st` / `.user.v1.st` | Redação com ferramentas numéricas |
| `responder-documental.system.v1.st` / `.user.v1.st` | Redação a partir de `<trechos>` rotulados `T1..Tn` |
| `redigir-analise.system.v1.st` / `.user.v1.st` | Resumo e recomendações da análise |

Delimitadores: `<historico>`, `<trechos>`, `<numeros>`, `<pergunta>`. Conteúdo de trechos, histórico
e resposta de agente remoto é dado, não instrução. A versão do prompt vai para métricas e logs.

## 5. agente-riscos: portas

| Porta / componente | Contrato |
|---|---|
| `AvaliarRiscoService` (aplicação) | `ParecerRisco avaliar(IndicadoresFinanceiros)`; aplica todas as `RegraRisco` registradas. Determinístico. |
| `FiltroTokenAgente` (infraestrutura.seguranca) | Exige `Authorization: Bearer` igual a `A2A_TOKEN_RISCOS` no endpoint JSON-RPC; compara em tempo constante; 401 sem detalhe; Agent Card público. |
| `ExecutorAvaliacaoRisco` (infraestrutura.a2a) | Implementa o executor de agente do SDK A2A: extrai o `DataPart`, valida contra o schema, chama o serviço, devolve artefato `DataPart` com o parecer. Entrada inválida → tarefa com estado de falha e mensagem sem detalhe interno. |
