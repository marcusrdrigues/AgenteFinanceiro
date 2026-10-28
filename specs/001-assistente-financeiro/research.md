# Pesquisa (Fase 0): Assistente Financeiro da FinanceCorp

**Plano**: [plan.md](plan.md) | **Data da pesquisa**: 2026-10-27

Versões conferidas em fonte primária (Maven Central, documentação oficial, GitHub releases, Docker
Hub) em 2026-10-27. Itens **NÃO CONFERIDO** precisam de verificação no incremento indicado antes
de virar código.

## 1. Versões fixadas

| Item | Versão / coordenada | Fonte | Conferido |
|---|---|---|---|
| Java | 21 (LTS) | constituição | — |
| Spring Boot | `4.1.2` (parent `org.springframework.boot:spring-boot-starter-parent`) — traz Spring Framework 7.0.10, Testcontainers 2.0.5, driver MongoDB 5.8.1, Jackson 3.1.7 | pom `spring-boot-dependencies:4.1.2` no Maven Central | Sim |
| Spring AI | BOM `org.springframework.ai:spring-ai-bom:2.0.2` (GA, 2026-10-22). "Spring AI 2.0.x supports Spring Boot 4.0.x and 4.1.x"; o starter 2.0.2 depende de Boot 4.1.2 | docs.spring.io/spring-ai/reference/getting-started.html; Maven Central | Sim |
| Starter OpenAI (chat + embedding) | `spring-ai-starter-model-openai` (versão pelo BOM) | Maven Central; reference `openai-chat.html` | Sim |
| Starter PgVector | `spring-ai-starter-vector-store-pgvector` | Maven Central; reference `vectordbs/pgvector.html` | Sim |
| Memória MongoDB | `spring-ai-starter-model-chat-memory-repository-mongodb`; classe `MongoChatMemoryRepository`; propriedades `spring.ai.chat.memory.repository.mongo.ttl` (segundos) e `.create-indices` | reference `api/chat-memory.html` | Sim |
| A2A Java SDK | BOM `org.a2aproject.sdk:a2a-java-sdk-bom:1.4.0.Final` (2026-09-28); cliente `a2a-java-sdk-client` (inclui JSON-RPC); protocolo A2A **v1.0** (com camada de compatibilidade v0.3) | github.com/a2aproject/a2a-java/releases/tag/v1.4.0.Final; docs `1_4_0_Final/client.md`, `server.md`, `compatibility.md` | Sim |
| Spring AI A2A (comunidade) | `org.springaicommunity:spring-ai-a2a-server-autoconfigure:0.4.0` (2026-10-23); compilado com Spring AI 2.0.1, Boot 4.1.1, A2A SDK 1.4.0.Final; ativa com `spring.ai.a2a.server.enabled=true`; Agent Card em `/.well-known/agent-card.json` | github.com/spring-ai-community/spring-ai-a2a/releases/tag/v0.4.0; Maven Central | Sim |
| Resilience4j | `io.github.resilience4j:resilience4j-spring-boot4:2.4.0` (compilado contra Boot 4.0.0) | Maven Central | Versão: Sim. Compatibilidade com Boot 4.1: **NÃO CONFERIDO** (incremento 7) |
| Flyway | versão gerenciada pelo Boot 4.1.2 (`flyway-core` + `flyway-database-postgresql`) | `spring-boot-dependencies:4.1.2` | Versão exata: **conferir no pom gerado** (incremento 1) |
| Testcontainers | `2.0.5` (gerenciado pelo Boot): `testcontainers-postgresql`, `testcontainers-mongodb`; classes `org.testcontainers.postgresql.PostgreSQLContainer`, `org.testcontainers.mongodb.MongoDBContainer`; `@ServiceConnection` cobre os dois | Maven Central; docs.spring.io/spring-boot/reference/testing/testcontainers.html | Sim |
| WireMock | `org.wiremock:wiremock-standalone:3.13.2` (linha 4.x ainda beta) | Maven Central | Versão: Sim. Compatibilidade com Boot 4.1: **NÃO CONFERIDO** (incremento 2) |
| ArchUnit | `com.tngtech.archunit:archunit-junit5` | — | Versão: **NÃO CONFERIDO** (incremento 0) |
| Imagem PostgreSQL + pgvector | `pgvector/pgvector:0.8.7-pg18` | Docker Hub | Sim |
| Imagem MongoDB | `mongo:8.0.34` (série 8.0; 8.3.13 também existe, política de suporte não conferida) | Docker Hub | Sim |
| springdoc-openapi | `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.2.0` (2026-10-24, compilado contra Boot 4.1.2); exige `swagger-annotations`/`-jakarta` 2.2.55, fixadas na raiz (D15) | Maven Central (pom `springdoc-openapi:3.2.0`) | Sim |
| Imagem pgAdmin | `dpage/pgadmin4:9.18.0` | Docker Hub | Sim |
| Embedding OpenAI | `text-embedding-3-small`, 1536 dimensões (o padrão do Spring AI é `text-embedding-ada-002`; por isso é fixado em configuração) | developers.openai.com/api/docs/guides/embeddings | Sim |
| Modelo de chat OpenAI | IDs atuais publicados: `gpt-6-astra`, `gpt-6.1-sol`, `gpt-6.1-terra`, `gpt-6-luna`. **Não há snapshot datado** publicado | developers.openai.com/api/docs/models | IDs: Sim. Snapshot datado: **NÃO CONFERIDO** (não existe) |

## 2. Decisões

### D1. Linha Spring AI 2.0.2 + Spring Boot 4.1.2

- **Decisão**: Spring AI 2.0.2 (GA mais recente) sobre Spring Boot 4.1.2, a versão de Boot que o
  próprio starter 2.0.2 usa.
- **Racional**: o usuário pediu "Boot compatível com a versão do Spring AI escolhida"; usar a mesma
  versão de Boot que o starter declara elimina divergência de dependências transitivas.
- **Alternativas**: Spring AI 1.1.8 + Boot 3.x (descartada: linha anterior, API de configuração
  diferente e material da aula ficaria datado); 2.1.0-RC1 (descartada: pré-lançamento).
- **Atenção**: exemplos da linha 1.x (ex.: `spring.ai.openai.chat.options.*`) não valem; na 2.0 as
  opções ficam em `spring.ai.openai.chat.*` (conferido na 2.0.1; **reconferir na 2.0.2** no
  incremento 2).

### D2. Roteamento fixo com ferramentas restritas ao caminho numérico

- **Decisão**: workflow com passos fixos em código (degrau 3 da escada da `engenharia-ia.md`):
  classificar → caminho fixo por tipo. O `@Tool` existe e é usado **somente** no `ChatClient` do
  caminho `NUMERICA`/`MISTA`, com três ferramentas de leitura. Busca de documentos, análise e
  chamada ao agente de risco são feitas pelo código, não pelo modelo.
- **Racional**: o modelo precisa transformar linguagem natural ("e em abril?", "1º tri") em
  parâmetros de consulta — é aí que a chamada de ferramenta agrega valor e é o requisito didático
  de `@Tool`. Nos outros caminhos o código já sabe o que buscar; dar ferramenta ao modelo ali só
  aumentaria custo e superfície de injeção.
- **Alternativas**: agente com todas as ferramentas (busca, análise, A2A) decidindo sozinho
  (descartada: não determinístico, mais difícil provar FR-006/FR-007 e combina dado privado +
  conteúdo não confiável + canal de saída num só laço); classificador extraindo período e código
  chamando a consulta, sem `@Tool` (descartada só por perder o objetivo didático; segue como
  alternativa se o eval do incremento 2 mostrar tool calling instável).

### D3. Reescrita autônoma da pergunta para a memória (US03)

- **Decisão**: o classificador recebe o histórico recente e devolve `perguntaAutonoma` — a pergunta
  reescrita sem depender do histórico ("E em abril?" → "Qual foi a receita de abril de 2025?").
  Os passos seguintes usam só a pergunta autônoma. O histórico é lido e gravado em código pela
  interface `ChatMemory` (`MessageWindowChatMemory`, janela 20) sobre `MongoChatMemoryRepository`.
- **Conceito — reescrita de consulta**: transformar uma pergunta dependente de contexto numa
  pergunta completa antes da busca. Importa aqui porque a busca vetorial e as ferramentas não
  entendem "e em abril?", e porque assim só um passo vê o histórico.
- **Racional**: o `MessageChatMemoryAdvisor` grava automaticamente a resposta da chamada em que é
  usado — no classificador, gravaria o JSON de classificação como se fosse resposta ao usuário.
  Usar `ChatMemory` direto permite gravar só a pergunta e o texto final (data-model §1.3).
- **Alternativas**: advisor de memória em todos os `ChatClient`s (descartada: histórico duplicado
  em cada passo, mais tokens e mais superfície para o histórico influenciar números).
- **Observação**: o `MongoChatMemoryRepository` descarta mensagens de tool call — irrelevante aqui,
  já que só pergunta e resposta final entram na memória.
- **Retenção (FR-029)**: `spring.ai.chat.memory.repository.mongo.ttl=2592000` (30 dias). O TTL do
  MongoDB conta por mensagem, não por "última atividade da conversa": mensagens antigas de uma
  conversa ativa expiram antes das recentes. Como a janela considerada é de 20 mensagens, o efeito
  prático atende FR-029; o comportamento exato do índice TTL é verificado no incremento 4.

### D4. Números sempre do código; verificação antes de responder

- **Decisão**: `RegistroConsultas` por requisição, passado às ferramentas pelo contexto de
  ferramenta (`.toolContext(Map)`, conferido). Os campos estruturados da resposta (`valores`,
  `periodo`, `fontes`) são montados do registro. `VerificadorValoresResposta` extrai valores do
  texto redigido e reprova qualquer um ausente do registro (FR-007). Na análise (US04), receitas,
  despesas, saldo e risco são preenchidos pelo código; o modelo só redige resumo e recomendações.
- **Alternativas**: confiar no prompt "não invente números" (descartada: prompt não é controle);
  pedir ao modelo os valores em saída estruturada (descartada: o modelo continuaria sendo a fonte).

### D5. Saída estruturada nativa da OpenAI

- **Decisão**: `.call().entity(Record.class)` com saída estruturada nativa
  (`AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT` ou `spec.useProviderStructuredOutput()`,
  conferidos). Os records de saída do modelo têm objeto no topo (a OpenAI não aceita array no
  topo). Validação em código depois da conversão; até 2 novas tentativas.

### D6. RAG em PgVector com migração e filtro dentro da busca

- **Decisão**: `PgVectorStore` com `initialize-schema=false`, esquema por Flyway, HNSW +
  `COSINE_DISTANCE`, `dimensions=1536`, `text-embedding-3-small`. Filtro
  `situacao == 'AUTORIZADO' && modeloEmbedding == '<atual>'` via `FilterExpressionBuilder` na
  própria `SearchRequest`. Limiar de similaridade inicial `0.55` e `topK=4`, **a calibrar pelo eval
  de recuperação** no incremento 3. Divisor próprio com sobreposição (o `TokenTextSplitter` não
  tem sobreposição): 1.200 caracteres, sobreposição 200.
- **Ingestão**: os documentos fictícios ficam em `agente-financeiro/documentos/` (Markdown com
  front matter: título, versão, vigência, situação) e são carregados por um `ApplicationRunner`
  ativado só pelo perfil `ingestao` (spec: "processo administrativo prévio"). Não há endpoint de
  upload nesta versão.
- **Alternativas**: `initialize-schema=true` (descartada: esquema fora do controle de migração);
  filtro depois da busca (descartada: devolve menos que `k` e é fácil de esquecer).
- **NÃO CONFERIDO** (incremento 3): colunas exatas da tabela esperada pelo `PgVectorStore` 2.0.2 e
  como o `score` é calculado com `COSINE_DISTANCE` antes de fixar o limiar.

### D7. A2A: servidor com Spring AI A2A, cliente com o SDK puro

- **Decisão**:
  - `agente-riscos` (servidor): `spring-ai-a2a-server-autoconfigure:0.4.0` com um `AgentExecutor`
    próprio, só regras, sem modelo de linguagem; Agent Card em `/.well-known/agent-card.json`.
  - `agente-financeiro` (cliente): `a2a-java-sdk-client:1.4.0.Final` atrás de `AgenteRiscoPort`,
    chamado pelo código no caminho `RISCO_ESPECIALIZADO` (não como `@Tool`).
  - Dados trocados como parte de dados estruturada (JSON) com schema próprio
    ([contracts/a2a-avaliacao-risco.md](contracts/a2a-avaliacao-risco.md)).
- **Racional**: o SDK não tem integração oficial com Spring (só Quarkus/Jakarta); a integração
  Spring AI A2A da comunidade é o caminho pedido e evita fiação manual. Ela só oferece o lado
  servidor, então o cliente usa o SDK.
- **Riscos registrados**:
  1. Projeto comunitário, pré-1.0, marcado como "incubating".
  2. A autoconfiguração usa `@ConditionalOnClass(ChatClient.class)` — exige a classe no classpath,
     não um bean. A leitura do código indica que um executor sem LLM funciona sem chave; **não
     testado**.
  3. Compilado com Jackson 2 (`jackson-databind 2.21.5`), enquanto o Boot 4.1.2 usa Jackson 3.
     Pacotes diferentes, devem coexistir, mas **exige teste de integração**.
  4. README do projeto está desatualizado (cita SDK 0.3 e groupId `io.github.a2asdk`); vale o pom
     da tag v0.4.0.
- **Plano B** (se o incremento 6 falhar nos riscos 2 ou 3): fiação manual do SDK com beans Spring,
  caminho documentado em `server.md` do SDK 1.4.0. O contrato A2A não muda.
- **NÃO CONFERIDO** (incremento 6/7): assinatura exata de `Client.builder(card)`; caminho do
  endpoint JSON-RPC exposto pela autoconfiguração.
- **Alternativas**: endpoint REST próprio entre os agentes (proibido pela constituição, VIII);
  módulo Maven compartilhado com os records do contrato (descartada: acopla o build dos dois
  agentes, e o objetivo do A2A é justamente a independência pelo protocolo — o contrato é o JSON
  Schema, verificado por teste de contrato nos dois lados).

### D8. Resiliência da chamada ao especialista e limite de requisições

- **Decisão**: Resilience4j (`resilience4j-spring-boot4:2.4.0`) para `TimeLimiter` (5 s),
  `Retry` (1 nova tentativa, só erro transitório, backoff com variação aleatória),
  `CircuitBreaker` (5 falhas consecutivas → aberto 30 s) e `RateLimiter` (FR-012).
  **Dependência nova: precisa de aprovação.**
- **Alternativas**: `@Retryable` do Spring Framework 7 (conferido) + timeout no cliente HTTP +
  disjuntor escrito à mão (descartada como primeira opção: o Framework 7 não tem circuit breaker
  nem rate limiter, e escrever um disjuntor correto à mão é reinventar uma peça madura); manter
  como plano B se a compatibilidade de Resilience4j 2.4.0 com Boot 4.1 falhar.

### D9. Escolha do modelo de chat

- **Situação**: a OpenAI publica IDs sem snapshot datado (`gpt-6.1-sol`, `gpt-6-luna` etc.), o que
  conflita com a regra "modelo fixado por identificador com versão". Além disso, `gpt-6.1-terra`
  exige a Responses API para chamada de ferramenta, e `gpt-6-luna` só faz function calling em Chat
  Completions com reasoning effort `none`. **Não foi conferido** qual API o Spring AI 2.0.2 usa
  por baixo (usa o SDK oficial `openai-java` desde a 2.0.0-M5).
- **Decisão**: o ID fica em `OPENAI_CHAT_MODEL` (sem valor padrão no código). No incremento 2, um
  spike verifica tool calling com os candidatos `gpt-6-luna` (menor, para classificação) e
  `gpt-6.1-sol`, e o eval decide: vale o menor que passar na nota mínima (`engenharia-ia.md`).
  Como mitigação da falta de snapshot, cada execução do eval grava o ID do modelo e a data, e o
  eval é reexecutado antes de cada aula.
- **Parâmetros iniciais** (em configuração, com motivo): `temperature=0.0` (fidelidade aos números e
  trechos), `max-tokens=600` nas redações e `200` no classificador (teto de custo),
  `timeout=20s`, `max-retries=0` no cliente (retry em um único ponto, na aplicação).

### D10. Dados enviados ao provedor

- A OpenAI recebe pergunta, histórico recente, trechos de políticas e números agregados — todos
  **fictícios**. Não há dado pessoal real; por isso não se aplicam as exigências de contrato de
  operador e transferência internacional da LGPD. Se dados reais forem usados um dia, isso muda e
  exige revisão (constituição, "Restrições de Segurança e Dados").

### D11. Testes

- Unitário: JUnit 5, AssertJ, Mockito; domínio e casos de uso sem Spring.
- Arquitetura: ArchUnit — `dominio` e `aplicacao` sem `org.springframework.*` (exceto anotações de
  transação, se usadas) e sem `org.springframework.ai.*`. **Dependência nova: precisa de aprovação.**
- Integração: `@SpringBootTest` + Testcontainers (`pgvector/pgvector:0.8.7-pg18`, `mongo:8.0.34`)
  com `@ServiceConnection`; `EmbeddingModel` falso e determinístico; nenhum H2.
- Provedor OpenAI simulado com WireMock via `spring.ai.openai.base-url` (429, 500, timeout, JSON
  inválido, tool call). **Dependência nova (escopo de teste): precisa de aprovação.**
- Contrato A2A: `agente-riscos` sobe em porta aleatória e um cliente A2A real do SDK envia as
  mensagens do contrato; no `agente-financeiro`, o adaptador é testado contra um servidor A2A
  falso em WireMock que serve envelopes JSON-RPC gravados de uma troca real (spike do incremento
  6), com variantes controladas (válida, inválida, lenta, 500, 401, fora do ar). Nenhuma
  dependência de servidor A2A no escopo de teste do agente-financeiro.
- Eval: `@Tag("eval")`, perfil Maven `eval`, casos em `agente-financeiro/eval/*.jsonl`, modelo real,
  fora do CI.

### D12. Autenticação entre agentes (FR-044)

- **Decisão**: token compartilhado (`A2A_TOKEN_RISCOS`, ≥ 32 caracteres) em `Authorization:
  Bearer`, exigido por um `OncePerRequestFilter` no agente-riscos e enviado pelo cliente A2A do
  agente-financeiro; esquema `bearer` declarado no Agent Card; comparação em tempo constante.
  Sem dependência nova (sem Spring Security).
- **Alternativas**: Spring Security com resource server JWT (descartada: exige emissor de token e
  dependência nova para um único segredo local); mTLS (descartada para a aula: gerência de
  certificados; é o caminho recomendado para ambiente compartilhado); sem autenticação
  (descartada: viola a constituição VIII).
- **NÃO CONFERIDO** (incrementos 6 e 7): nome dos campos de esquema de segurança no Agent Card do
  SDK 1.4.0 e como o `Client` do SDK adiciona cabeçalhos (interceptador ou configuração do
  transporte).

### D13. Registro de execução (FR-036 a FR-038)

- **Decisão**: `RegistroExecucaoPort` com adaptador SLF4J que escreve uma linha JSON por
  requisição no logger `execucao`, com os campos de `RegistroExecucao` (data-model); arquivo local
  com rotação diária e `logging.logback.rollingpolicy.max-history=90`. Tokens de entrada e saída
  lidos dos metadados de uso da resposta do Spring AI (**conferir** o acesso na 2.0.2).
- **Alternativas**: tabela no Postgres (descartada: o papel da aplicação é só leitura e o log já
  cobre a retenção); só métricas do Micrometer (descartada: métricas não guardam o vínculo por
  requisição exigido por FR-036).

### D14. Novas tentativas do provedor de modelo

- **Decisão**: `ExecutorChamadaModelo` em `infraestrutura.ia` com `RetryTemplate` do Spring
  Framework 7 (conferido em research §1), `tentativas-transitorias=2`, backoff 500 ms ×2 com
  variação aleatória, só para `TransientAiException`; cliente OpenAI com `max-retries=0` para não
  multiplicar tentativas.

### D15. Swagger (springdoc) nos dois agentes

- **Decisão**: springdoc 3.2.0 publica `/v3/api-docs` e `/swagger-ui.html`; no agente-financeiro só
  `/api/**` entra no documento (actuator fica de fora). O OpenAPI estático em `contracts/` continua
  sendo o contrato de referência; o gerado serve para explorar e testar na aula. No agente-riscos,
  o Swagger descreve os endpoints HTTP e aponta para o contrato A2A — não substitui o protocolo
  (constituição VIII).
- **Conflito encontrado**: o SDK da OpenAI traz `swagger-annotations` 2.2.31 e o
  `spring-ai-client-chat` traz `swagger-annotations-jakarta` 2.2.38; com o springdoc (swagger-core
  2.2.55), `/v3/api-docs` respondia 500. Os dois artefatos foram fixados em 2.2.55 na raiz.
- **Alternativa**: só o OpenAPI estático (descartada: o usuário pediu Swagger em cada projeto).

### D16. pgAdmin no Docker Compose

- **Decisão**: `dpage/pgadmin4:9.18.0` em `127.0.0.1:5050`; login por `PGADMIN_EMAIL` /
  `PGADMIN_PASSWORD` do `.env` (valores combinados para a aula, nunca no repositório);
  `infra/pgadmin/servers.json` pré-cadastra o servidor com o papel `agente_leitura`, sem senha no
  arquivo.
- **Alternativa**: servidor pré-cadastrado com o superusuário (descartada: quem só precisa olhar
  não recebe permissão para alterar — mesmo critério de menor privilégio da aplicação).

## 3. Dependências que pedem aprovação

| Dependência | Para quê | Mantida? | Licença |
|---|---|---|---|
| `spring-ai-a2a-server-autoconfigure` 0.4.0 | Servidor A2A no agente-riscos | Sim, comunidade, pré-1.0 | Apache-2.0 (**conferir** no pom) |
| `a2a-java-sdk-*` 1.4.0.Final | Cliente/servidor A2A | Sim, projeto a2aproject (Linux Foundation) | Apache-2.0 (**conferir**) |
| `resilience4j-spring-boot4` 2.4.0 | Timeout, retry, disjuntor, limite de requisições | Sim | Apache-2.0 |
| `flyway-core` + `flyway-database-postgresql` | Migrações | Sim (Redgate) | Apache-2.0 (edição Community) |
| `wiremock-standalone` 3.13.2 (teste) | Simular OpenAI e falhas HTTP | Sim | Apache-2.0 |
| `archunit-junit5` (teste) | Teste de direção das dependências | Sim | Apache-2.0 |

As duas primeiras foram pedidas explicitamente pelo usuário na arquitetura técnica; as demais são
propostas deste plano.
