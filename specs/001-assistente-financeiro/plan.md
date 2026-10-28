# Plano de Implementação: Assistente Financeiro da FinanceCorp

**Branch**: `001-assistente-financeiro` | **Data**: 2026-10-27 | **Spec**: [spec.md](spec.md)

**Entrada**: especificação em `specs/001-assistente-financeiro/spec.md` e arquitetura técnica
definida pelo usuário.

## Resumo

Dois serviços Spring Boot independentes num repositório Maven multimódulo:

- **agente-financeiro** (porta 8080): API REST local que responde perguntas sobre receitas,
  despesas, saldo e políticas da FinanceCorp fictícia. Workflow fixo: o modelo classifica a
  pergunta e a reescreve de forma autônoma; cada tipo segue um caminho em código. Números vêm de
  ferramentas `@Tool` somente leitura sobre PostgreSQL e são verificados em código antes da
  resposta; políticas vêm de RAG em pgvector com fonte citada; histórico em MongoDB via
  `ChatMemory`; risco especializado pedido por A2A.
- **agente-riscos** (porta 8081): agente A2A sem modelo de linguagem que avalia risco por regras
  fixas sobre indicadores agregados.

Degrau da escada da `engenharia-ia.md`: **workflow com passos fixos** (degrau 3). Uma chamada só
(degrau 1) não basta porque há duas fontes de verdade distintas (registros e documentos) que não
podem se misturar; RAG puro (degrau 2) não resolve números; agente autônomo (degrau 4) foi
descartado (research D2).

## Contexto Técnico

**Linguagem/Versão**: Java 21

**Dependências principais** (versões exatas em [research.md](research.md) §1): Spring Boot 4.1.2;
Spring AI 2.0.2 (BOM) com `spring-ai-starter-model-openai`, `spring-ai-starter-vector-store-pgvector`,
`spring-ai-starter-model-chat-memory-repository-mongodb`; A2A Java SDK 1.4.0.Final
(`org.a2aproject.sdk`); Spring AI A2A server autoconfigure 0.4.0 (`org.springaicommunity`);
Resilience4j 2.4.0, Flyway 12.4.0 (gerenciado pelo Boot), springdoc-openapi 3.2.0 (Swagger UI) —
todos aprovados (`docs/decisoes/0001-dependencias.md`).

**Armazenamento**: PostgreSQL 18 + pgvector 0.8.7 (`pgvector/pgvector:0.8.7-pg18`) para lançamentos
e base vetorial; MongoDB 8.0 (`mongo:8.0.34`) para memória conversacional. Ambos locais em Docker
Compose; nada em nuvem. O agente-riscos não tem banco. **pgAdmin** (`dpage/pgadmin4:9.18.0`) no
mesmo Compose, em `127.0.0.1:5050`, com credenciais no `.env` e servidor pré-cadastrado com o papel
`agente_leitura` (menor privilégio: inspecionar sem alterar).

**Testes**: JUnit 5, AssertJ, Mockito, ArchUnit (a aprovar), Testcontainers 2.0.5 com
`@ServiceConnection`, WireMock 3.13.2 (a aprovar), eval com `@Tag("eval")` em perfil Maven próprio.

**Plataforma alvo**: máquina local do aluno (Windows, macOS ou Linux) com Docker; JVM 21.

**Tipo de projeto**: dois web services (multimódulo Maven).

**Metas de desempenho**: SC-008 — 95% das consultas numéricas/documentais em até 10 s; análise em
até 30 s. Pedido de risco com especialista fora do ar responde em até 15 s (SC-011).

**Restrições**: só `localhost`, sem autenticação (spec, "Restrições da aula"); dados fictícios;
nenhuma escrita em dado financeiro; segredos só por variável de ambiente; limites de consumo em
código (abaixo).

**Escala/Escopo**: uso individual em aula (1 usuário por máquina); ~12 meses de lançamentos
fictícios (algumas centenas de linhas); ~6 documentos de política fictícios.

### Limites de consumo (constituição, "Restrições de Segurança e Dados")

| Limite | Valor inicial | Onde |
|---|---|---|
| Tamanho da pergunta | 1.000 caracteres | Bean Validation no DTO (413/400) |
| Requisições | 20/min por `conversaId` e 60/min no total | Resilience4j `RateLimiter` (429) |
| Tokens de saída | 200 (classificador), 600 (redações) | `spring.ai.openai.chat.max-tokens` por cliente |
| Chamadas de ferramenta por requisição | 4 | contador no `RegistroConsultas` |
| Novas tentativas de saída inválida | 2 | `financeiro.ia.tentativas-validacao` |
| Tempo total por requisição | 25 s (análise: 30 s) | orçamento de tempo no `ResponderPerguntaService` |
| Histórico enviado ao classificador | 20 mensagens | `MessageWindowChatMemory` |
| Trechos no contexto | `topK=4`, 1.200 caracteres cada | `financeiro.rag.*` |
| Período por consulta | 366 dias | `Periodo` |

## Verificação da Constituição

*Gate: precisa passar antes da Fase 0. Reconferido após a Fase 1.*

| Princípio | Como o plano atende | Situação |
|---|---|---|
| I. Stack fixa | Java 21, Boot 4.1.2, Spring AI 2.0.2 por BOM, Maven; versões fixas; dependências novas listadas para aprovação (research §3) | ✅ (aprovação pendente das dependências) |
| II. Camadas com dependência para dentro | `dominio` → `aplicacao` (portas) → `infraestrutura` / `interfaces`; ArchUnit garante | ✅ |
| III. Incremental e estável | 8 incrementos, cada um executável e verificável; incremento só começa com o anterior validado | ✅ |
| IV. BigDecimal e testes | `Dinheiro` com escala 2 e HALF_EVEN; `numeric(15,2)`; valores como texto na API e no A2A; testes de borda obrigatórios | ✅ |
| V. Modelo não inventa números | Roteamento fixo; números só de ferramentas/código; `VerificadorValoresResposta`; análise com números do código; eval com casos de indução | ✅ |
| VI. RAG rastreável | Metadados por trecho, fonte derivada da busca, mensagem fixa sem fonte, filtro na busca, eval de recuperação separado | ✅ |
| VII. Somente leitura | Só ferramentas de leitura; papel `agente_leitura` só com `SELECT`; nenhuma integração bancária. **Exceção**: autorização por identidade de sessão não existe (sem autenticação) | ⚠️ Justificado em Complexidade |
| VIII. A2A | Servidor Spring AI A2A + cliente SDK; nenhum REST entre agentes; autenticação entre agentes por token compartilhado em `Authorization: Bearer` (research D12); teste de contrato nos dois lados; resposta remota validada e tratada como não confiável | ✅ |
| VI. RAG (remoção e origem) | Trecho guarda `arquivoOrigem` e `cadastradoPor`; carga sincroniza o conjunto curado: arquivo removido apaga documento, trechos e vetores; versão nova desautoriza a anterior (FR-045) | ✅ |
| Modelo versionado (Restrições) | A OpenAI não publica snapshot datado (research D9) | ⚠️ Justificado em Complexidade |
| IX. Segredos e logs | `OPENAI_API_KEY` só por ambiente, `.env` no `.gitignore`, `.env.example` só com nomes; MDC `traceId`; logs só com metadados; ProblemDetail sem detalhe interno; log de prompt desligado | ✅ |
| Restrições de dados | Dados fictícios; tríade "dado privado + conteúdo não confiável + saída externa" mitigada: dados fictícios, ferramentas só leitura, A2A só recebe agregados, saída do especialista nunca vira instrução | ✅ |
| Fluxo e critérios de pronto | Cada incremento fecha com testes passando, eval (quando há modelo) e docs Markdown | ✅ |

**Resultado do gate**: aprovado, com duas exceções justificadas (VII e modelo versionado) e aprovações de dependência
pendentes que **bloqueiam o início do incremento que usa cada uma**.

## Estrutura do Projeto

### Documentação (desta funcionalidade)

```text
specs/001-assistente-financeiro/
├── plan.md                 # este arquivo
├── research.md             # Fase 0: versões e decisões
├── data-model.md           # Fase 1: entidades, tabelas, records
├── quickstart.md           # Fase 1: guia de validação ponta a ponta
├── contracts/
│   ├── api-agente-financeiro.yaml     # OpenAPI da API REST
│   ├── a2a-avaliacao-risco.md         # conteúdo trocado por A2A
│   ├── portas-e-ferramentas.md        # portas, @Tool, verificação, prompts
│   └── exemplos/                      # JSON de referência do contrato A2A
└── tasks.md                # Fase 2 (/speckit-tasks; não criado aqui)
```

### Código-fonte (raiz do repositório)

```text
pom.xml                              # agregador + parent: spring-boot-starter-parent 4.1.2,
                                     # dependencyManagement com spring-ai-bom 2.0.2 e
                                     # a2a-java-sdk-bom 1.4.0.Final, propriedades de versão
docker-compose.yml                   # postgres-pgvector:5432, mongo:27017, pgadmin:5050 (só 127.0.0.1)
infra/pgadmin/servers.json           # servidor pré-cadastrado no pgAdmin (agente_leitura, sem senha)
infra/postgres/init/01-papeis.sh    # cria papéis agente_migracao, agente_ingestao e agente_leitura
.env.example                         # nomes das variáveis, sem valores
docs/                                # roteiro da aula por incremento, criterios-risco.md, ADRs

agente-financeiro/
├── pom.xml
├── documentos/                      # políticas fictícias (Markdown + front matter)
├── eval/                            # numerico.jsonl, recuperacao.jsonl, resposta.jsonl,
│                                    # conversa.jsonl, adversarial.jsonl, resultados/
└── src/
    ├── main/java/br/com/financecorp/agentefinanceiro/
    │   ├── dominio/                 # Dinheiro, Periodo, Lancamento, Categoria, ResumoFinanceiro,
    │   │                            # AvaliadorRiscoPorRegras, TipoPergunta, ConversaId, exceções
    │   ├── aplicacao/
    │   │   ├── porta/               # interfaces de saída (contracts/portas-e-ferramentas.md)
    │   │   ├── servico/             # ResponderPerguntaService, ConsultarResumoFinanceiroService,
    │   │   │                        # ConsultarPoliticaService, GerarAnaliseFinanceiraService,
    │   │   │                        # SolicitarAvaliacaoRiscoService, IngerirDocumentoService
    │   │   ├── contexto/            # MontadorContexto, DivisorTexto (Java puro)
    │   │   └── validacao/           # VerificadorValoresResposta, ValidadorClassificacao
    │   ├── infraestrutura/
    │   │   ├── persistencia/        # JdbcRepositorioLancamentoAdapter, JdbcRepositorioDocumentoAdapter
    │   │   ├── vetorial/            # PgVectorBuscaDocumentoAdapter, PgVectorGravacaoAdapter
    │   │   ├── ia/                  # adaptadores OpenAI (ChatClient), carregador de prompts
    │   │   │   └── ferramentas/     # FerramentasFinanceiras (@Tool)
    │   │   ├── memoria/             # SpringAiMemoriaConversaAdapter (ChatMemory + Mongo)
    │   │   ├── a2a/                 # A2aAgenteRiscoAdapter (cliente SDK + Resilience4j)
    │   │   ├── ingestao/            # IngestaoDocumentosRunner (perfil "ingestao")
    │   │   └── config/              # @ConfigurationProperties (records @Validated), beans
    │   └── interfaces/web/          # PerguntaController, DTOs (records), ErroHandler, FiltroTraceId,
    │                                # FiltroLimiteRequisicoes (Resilience4j RateLimiter)
    ├── main/resources/
    │   ├── application.yml
    │   ├── prompts/                 # *.v1.st
    │   └── db/migration/            # V1__esquema_financeiro.sql, V2__lancamentos_ficticios.sql,
    │                                # V3__esquema_conhecimento.sql
    └── test/
        ├── java/.../                # *Test (unitário), *IT (integração), ArquiteturaTest,
        │                            # *ContratoA2aIT, eval/*EvalIT (@Tag("eval"))
        └── resources/contrato-a2a/  # cópia dos exemplos do contrato

agente-riscos/
├── pom.xml
└── src/
    ├── main/java/br/com/financecorp/agenteriscos/
    │   ├── dominio/                 # IndicadoresFinanceiros, RegraRisco (Strategy) e regras,
    │   │                            # Achado, ParecerRisco, NivelRisco
    │   ├── aplicacao/               # AvaliarRiscoService, ValidadorSolicitacao
    │   ├── infraestrutura/a2a/      # ExecutorAvaliacaoRisco (AgentExecutor), AgentCard, mapeadores
    │   ├── infraestrutura/seguranca/ # FiltroTokenAgente (Bearer, tempo constante)
    │   └── infraestrutura/config/
    ├── main/resources/application.yml
    └── test/
        ├── java/.../                # regras (unitário), ArquiteturaTest, ServidorA2aContratoIT
        └── resources/contrato-a2a/
```

**Decisão de estrutura**: um repositório, dois módulos Maven sem dependência entre si; o `pom.xml`
raiz só agrega e centraliza versões. Descartei um módulo `contrato-compartilhado` porque acoplaria
o build dos agentes; o contrato é o JSON Schema + exemplos, conferido por teste nos dois lados.

## Por módulo

### agente-financeiro

**Dependências** (sem versão no módulo; tudo pelo parent/BOM): `spring-boot-starter-web`,
`spring-boot-starter-validation`, `spring-boot-starter-actuator`, `spring-boot-starter-jdbc`,
`postgresql`, `flyway-core`, `flyway-database-postgresql`, `spring-ai-starter-model-openai`,
`spring-ai-starter-vector-store-pgvector`, `spring-ai-starter-model-chat-memory-repository-mongodb`,
`a2a-java-sdk-client`, `resilience4j-spring-boot4`. Teste: `spring-boot-starter-test`,
`spring-boot-testcontainers`, `testcontainers-postgresql`, `testcontainers-mongodb`,
`testcontainers-junit-jupiter`, `wiremock-standalone`, `archunit-junit5`.

**Endpoints**:

| Método e caminho | Função | Contrato |
|---|---|---|
| `POST /api/v1/conversas/{conversaId}/perguntas` | Pergunta numa conversa | [api-agente-financeiro.yaml](contracts/api-agente-financeiro.yaml) |
| `GET /actuator/health` | Saúde (inclui Postgres e Mongo; especialista **não** entra no health para não derrubar o status) | Spring Boot |
| `GET /v3/api-docs`, `GET /swagger-ui.html` | OpenAPI gerado pelo springdoc (só `/api/**`) e Swagger UI | springdoc 3.2.0 |

**Fluxo de `ResponderPerguntaService`**:

1. Valida `ConversaId` (UUID v4) e limites; abre orçamento de tempo.
2. Lê histórico (`MemoriaConversaPort`).
3. Classifica + reescreve (`ClassificadorPerguntaPort`), validação e até 2 novas tentativas.
4. Caminho por `TipoPergunta`:
   - `NUMERICA`: `RespondedorNumericoPort` com ferramentas → `VerificadorValoresResposta`.
   - `DOCUMENTAL`: `BuscaDocumentoPort` → vazio: mensagem fixa (sem modelo) → senão
     `MontadorContexto` → `RedatorDocumentalPort` → valida referências.
   - `MISTA`: os dois acima; resposta com origem por parte.
   - `ANALISE`: `ConsultarResumoFinanceiroService` (período atual e anterior) →
     `AvaliadorRiscoPorRegras` → busca de políticas → `RedatorAnalisePort` (resumo e
     recomendações) → monta as seis seções em código.
   - `RISCO_ESPECIALIZADO`: resumo → `AgenteRiscoPort`; falha → regras internas rotuladas.
   - `FORA_DO_DOMINIO`: resposta fixa.
5. Registra pergunta e texto final na memória; devolve `RespostaAssistente`.

**Tratamento de erros**: exceções de domínio → `ErroHandler` (`@RestControllerAdvice`) →
ProblemDetail com `type` em `https://financecorp.example/problemas/<codigo>`, `title` em
português, `traceId`, sem stack trace; 404 de rota também em ProblemDetail
(`spring.mvc.problemdetails.enabled=true`). Erro transitório do provedor: até 2 novas tentativas
num único ponto (research D14).

**Registro de execução**: uma linha JSON por requisição no logger `execucao` (research D13),
arquivo `logs/agente-financeiro-execucao.log` com rotação diária e 90 dias de retenção.

**Ingestão**: perfil `ingestao` com datasource próprio (`agente_ingestao`); sincroniza
`documentos/` com a base (insere, desautoriza versão anterior, apaga removidos). Exceções do Spring AI traduzidas no adaptador:
`TransientAiException` → retry no ponto único (aplicação) → `ModeloIndisponivelException` (503);
`NonTransientAiException` → sem retry → 503 com log de configuração sem a chave. Situações
funcionais (sem dados, sem documento, fora do domínio, especialista indisponível) são 200.

### agente-riscos

**Dependências**: `spring-boot-starter-web`, `spring-boot-starter-actuator`,
`spring-boot-starter-validation`, `spring-ai-a2a-server-autoconfigure` (traz o SDK A2A e o
cliente de chat do Spring AI apenas como classe no classpath, sem starter de modelo e sem chave).
Teste: `spring-boot-starter-test`, `a2a-java-sdk-client`, `archunit-junit5`.

**Endpoints**:

| Caminho | Função |
|---|---|
| `GET /.well-known/agent-card.json` | Agent Card (A2A), público, declara esquema `bearer` |
| endpoint JSON-RPC do A2A (caminho da autoconfiguração; conferir no incremento 6) | `message/send` e tarefas; exige `Authorization: Bearer ${A2A_TOKEN_RISCOS}` (401 sem detalhe) |
| `GET /actuator/health` | Saúde |
| `GET /v3/api-docs`, `GET /swagger-ui.html` | OpenAPI e Swagger UI (endpoints HTTP do serviço; o contrato entre agentes é o A2A) |

**Tratamento de erros**: requisição sem token ou com token errado → HTTP 401 sem detalhe, antes de
qualquer processamento; solicitação fora do schema ou incoerente → tarefa em estado de falha com
mensagem "solicitação inválida"; erro inesperado → `A2AError` genérico, sem detalhe interno; log com
`solicitacaoId` e código do erro, sem valores.

## Estratégia de testes

| Camada | agente-financeiro | agente-riscos |
|---|---|---|
| Unitário | `Dinheiro` (escala, arredondamento, formatação, negativos), `Periodo` (fábricas, limites, datas inválidas), `AvaliadorRiscoPorRegras` (cada indicador e nível), `VerificadorValoresResposta`, `ValidadorClassificacao`, `MontadorContexto` (limiar, ordem, neutralização de delimitadores, limites, rótulos), `DivisorTexto`, `ResponderPerguntaService` com fakes (busca vazia não chama redator; ferramenta nunca chamada fora de `NUMERICA`/`MISTA`; fallback de risco) | Cada `RegraRisco`, pontuação → nível, `ValidadorSolicitacao` (schema, coerência, número JSON recusado), justificativa por modelo de texto fixo |
| Arquitetura | `dominio`/`aplicacao` sem Spring AI, sem JDBC, sem A2A | `dominio`/`aplicacao` sem SDK A2A |
| Integração | Postgres+pgvector e Mongo via Testcontainers: migrações reais, papel só leitura recusa `UPDATE`, busca só `AUTORIZADO`, TTL configurado, embedding falso determinístico | — |
| HTTP | `@WebMvcTest`: 200, 400 (UUID v3, texto livre, corpo extra), 413, 429 com `Retry-After`, 503, ProblemDetail sem detalhe interno | — |
| Falhas | WireMock como OpenAI (429, 500, timeout, JSON inválido, tool call para ferramenta inexistente); WireMock como servidor A2A com envelopes gravados (lento, 500, 401, fora do ar, schema inválido); disjuntor abre e fecha | Entrada inválida → estado de falha; sem token / token errado → 401 |
| Contrato A2A | Adaptador cliente contra servidor de teste; exemplos iguais aos de referência | Servidor real em porta aleatória + cliente SDK real |
| Injeção (determinístico) | Trecho com "ignore as instruções" não altera o prompt; `</trechos>` neutralizado; resposta falsa do modelo com valor inventado é barrada; justificativa do especialista com instrução não chega ao modelo | — |
| Eval (fora do CI) | ≥ 30 casos: numérico, recuperação, resposta documental, conversa, análise, adversarial; notas mínimas SC-001…SC-006, SC-010 | — (determinístico, coberto por teste) |

Comandos: `./mvnw verify` (tudo, Docker ativo); `./mvnw -pl agente-financeiro verify -Peval` (eval,
exige `OPENAI_API_KEY` e `OPENAI_CHAT_MODEL`).

## Incrementos

> **Atualização (2026-10-27)**: a ordem de execução foi redefinida pelo usuário em
> [tasks.md](tasks.md) em 7 incrementos (bootstrap → agente base e RAG → memória → ferramentas →
> saída estruturada → agente de riscos A2A → orquestração). A tabela abaixo continua válida para
> o conteúdo e a verificação de cada entrega; vale a ordem de `tasks.md`.

Cada incremento termina com: build verde (`./mvnw verify`), critérios listados verificados, doc em
`docs/incremento-NN.md` e, se houver modelo, eval rodado e resultado gravado em
`agente-financeiro/eval/resultados/`.

| # | Entrega | História | Verificação |
|---|---|---|---|
| 0 | Esqueleto: pom raiz, dois módulos que sobem (8080/8081), `docker-compose.yml` com Postgres e Mongo em `127.0.0.1`, `.env.example`, `.gitignore`, `ArquiteturaTest` nos dois módulos | — | `docker compose up -d`; `/actuator/health` UP nos dois; subida sem `OPENAI_API_KEY` falha citando o nome da variável |
| 1 | Núcleo numérico determinístico: `Dinheiro`, `Periodo`, `Categoria`, migrações V1/V2 com lançamentos fictícios, papel só leitura, `ConsultarResumoFinanceiroService` | US02 (sem LLM) | Testes unitários de borda; IT confere totais de jan/2025, 1º tri e mês sem dados contra SQL direto; `UPDATE` com `agente_leitura` falha |
| 2 | Pergunta numérica por linguagem natural: endpoint POST, classificador, `FerramentasFinanceiras` (`@Tool`), `RegistroConsultas`, verificador de valores, ProblemDetail, rate limit, `traceId`; spike de modelo (research D9) | US02 | Cenários 1–7 de US02 em teste com modelo falso (WireMock); eval numérico ≥ 100% SC-001; modelo escolhido registrado |
| 3 | RAG: V3 (documento + vector_store), divisor, ingestão por perfil `ingestao`, busca filtrada, montador de contexto, mensagem fixa sem fonte, caminho `MISTA` | US01 | Cenários 1–6 de US01; IT: não autorizado nunca aparece; eval de recuperação ≥ 90% (SC-003) e de recusa 100% (SC-002); limiar calibrado e registrado |
| 4 | Memória: `ChatMemory` + Mongo, reescrita autônoma, TTL 30 dias | US03 | Cenários 1–6 de US03; IT: conversas isoladas, janela de 20, índice TTL presente; eval de conversa ≥ 90% (SC-004) |
| 5 | Análise estruturada: `AvaliadorRiscoPorRegras`, `GerarAnaliseFinanceiraService`, redator de análise | US04 | Cenários 1–6 de US04; números da análise idênticos aos de US02 (teste); SC-006 |
| 6 | agente-riscos: regras, `AgentExecutor`, Agent Card, teste de contrato com cliente SDK real; valida riscos da integração comunitária (research D7) ou aciona o plano B | US05 (lado servidor) | `curl` no Agent Card; `ServidorA2aContratoIT` verde; sobe sem chave de modelo |
| 7 | Integração A2A no agente-financeiro: adaptador cliente, Resilience4j (timeout, retry, disjuntor), fallback por regras internas | US05 | Cenários 1–7 de US05; com o agente-riscos parado, US01–US04 seguem funcionando e risco responde em ≤ 15 s (SC-011); SC-007 |
| 8 | Fechamento: eval adversarial completo (SC-005), inspeção automática de logs (SC-009), medição de tempo (SC-008), quickstart revisado | Todas | Todas as notas mínimas atingidas; `quickstart.md` executado do zero numa máquina limpa |

Dependências: 1 → 2 → 3 → 4 → 5 → 7; 6 depende só de 0 e pode ser feito em paralelo a 1–5; 7
depende de 5 e 6.

## Acompanhamento de Complexidade

| Violação | Por que é necessária | Alternativa mais simples descartada porque |
|---|---|---|
| Princípio VII: autorização de ferramenta "com a identidade da sessão autenticada" não existe (sistema sem autenticação) | Decisão do usuário para a aula: ambiente só local e dados fictícios | Autenticação local (ex.: usuário fixo com senha) adicionaria configuração sem proteger nada real neste cenário. Compensações: bind em `127.0.0.1` (API e bancos), UUID v4 não adivinhável, ferramentas sem escrita, aviso "não apto para produção" no README. Reavaliar antes de qualquer ambiente compartilhado. |
| Restrição "modelo fixado por identificador com versão": a OpenAI publica só IDs sem snapshot datado | Provedor definido pelo usuário (OpenAI); não há identificador mais estável disponível | Trocar de provedor contraria a arquitetura pedida. Mitigação: ID em `OPENAI_CHAT_MODEL`, sem padrão no código; cada execução do eval grava ID e data; eval completo reexecutado antes de cada aula e a cada mudança percebida de comportamento; queda abaixo da nota mínima bloqueia o uso. |
| Dependência comunitária pré-1.0 (`spring-ai-a2a`) | Pedido explícito do usuário; evita fiação manual do servidor A2A | Fiação manual do SDK fica como plano B documentado (research D7), acionado se o incremento 6 falhar. |
