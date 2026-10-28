# 0001 — Dependências aprovadas

**Data**: 2026-10-27 | **Status**: aprovada pelo usuário | **Tarefa**: T001

## Aprovadas

| Dependência | Versão | Para quê | Licença | Entra em |
|---|---|---|---|---|
| Flyway (`spring-boot-starter-flyway` + `flyway-database-postgresql`) | 12.4.0 (gerenciada pelo Boot 4.1.2) | Migrações do PostgreSQL | Apache-2.0 (Community) | Incremento 1 |
| ArchUnit (`com.tngtech.archunit:archunit`, teste) | 1.5.1 (última no Maven Central em 2026-10-27) | Direção das dependências entre camadas | Apache-2.0 | Incremento 1 |
| Resilience4j (`resilience4j-spring-boot4`) | 2.4.0 | Rate limit; timeout, retry e disjuntor do A2A | Apache-2.0 | Incrementos 2 e 7 |
| WireMock (`wiremock-standalone`, teste) | 3.13.2 | Simular OpenAI e servidor A2A | Apache-2.0 | Incremento 2 |

Aprovada em 2026-10-27, no incremento 2 (pedido do usuário: documentação Swagger em cada projeto):

| Dependência | Versão | Para quê | Licença | Entra em |
|---|---|---|---|---|
| springdoc (`springdoc-openapi-starter-webmvc-ui`) | 3.2.0 (compilado contra Boot 4.1.2) | OpenAPI em `/v3/api-docs` e Swagger UI em `/swagger-ui.html` nos dois agentes | Apache-2.0 | Incremento 2 |

Já pedidas na arquitetura técnica (licença conferida no pom do Maven Central):

| Dependência | Versão | Licença |
|---|---|---|
| `org.a2aproject.sdk:a2a-java-sdk-*` | 1.4.0.Final | Apache License 2.0 (pom `a2a-java-sdk-parent`) |
| `org.springaicommunity:spring-ai-a2a-server-autoconfigure` | 0.4.0 | Apache License 2.0 |

## Alinhamento de versões transitivas

- **`swagger-annotations` e `swagger-annotations-jakarta` fixadas em 2.2.55** (versão do
  swagger-core do springdoc 3.2.0). O SDK da OpenAI traz `swagger-annotations` 2.2.31 e o
  `spring-ai-client-chat` traz `swagger-annotations-jakarta` 2.2.38; os dois jars têm o mesmo pacote,
  e o `/v3/api-docs` respondia 500 (`NoSuchMethodError: Schema.$dynamicRef()`). Não é dependência
  nova: só a versão das que já vinham.
- **`spring-ai-retry` não está no classpath** no Spring AI 2.0.2 com o starter da OpenAI (o modelo
  usa o SDK oficial `openai-java`). O tradutor de exceções trata `com.openai.errors.*`; não foi
  adicionado o `spring-ai-retry`.

## Desvios registrados na implementação

- **ArchUnit**: usado o artefato `archunit` (núcleo) com testes JUnit comuns, em vez de
  `archunit-junit5`. O Boot 4.1.2 traz JUnit Jupiter 6.0.3, e o núcleo não depende do motor de
  testes do ArchUnit; mesma licença e mesma versão.
- **`a2a-java-sdk-bom` não é importado.** O BOM 1.4.0.Final gerencia `okhttp`,
  `junit-jupiter-params` e `weld-se-core` com `<scope>test</scope>`. Importado no pom raiz, ele
  rebaixou o `okhttp` (dependência de runtime do SDK da OpenAI) para teste: os testes passavam e o
  jar não subia (`NoClassDefFoundError: okhttp3/Callback`). Os artefatos A2A passam a usar
  `${a2a-sdk.version}` explicitamente. Lição: o checkpoint de cada incremento inclui subir o jar
  empacotado, não só o `verify`.
