# Incremento 2 — Agente base e RAG (US01)

**Data**: 2026-10-27 | **Tarefas**: T034–T066 | **Status**: implementado e testado; **eval com
modelo real pendente** (T066, falta `OPENAI_API_KEY`)

## O que foi entregue

- **Endpoint** `POST /api/v1/conversas/{conversaId}/perguntas` (contrato em
  `specs/001-assistente-financeiro/contracts/api-agente-financeiro.yaml`), documentado no Swagger.
- **Classificação com lista fechada** (`OpenAiClassificadorAdapter`, prompt `classificar.v1`):
  saída estruturada nativa, validada em código (`ValidadorClassificacao`); inválida é refeita com
  o motivo no máximo 2 vezes e depois vira `NAO_INTERPRETADA`.
- **Caminho DOCUMENTAL** (`ConsultarPoliticaService`): busca filtrada no pgvector → montagem do
  contexto em código (`MontadorContexto`) → redação (`OpenAiRedatorDocumentalAdapter`, prompt
  `responder-documental.v1`) → validação de referências e valores (`ValidadorReferencias`). Sem
  trecho acima do limiar: mensagem fixa, **sem chamar o modelo** (FR-020). Fontes derivadas da
  busca, nunca do texto do modelo.
- `FORA_DO_DOMINIO` com recusa fixa; os demais tipos respondem "funcionalidade ainda não
  disponível nesta etapa" até seus incrementos.
- **Ingestão** do conjunto curado (`agente-financeiro/documentos/`, 6 políticas fictícias) pelo
  perfil `ingestao`, com o papel `agente_ingestao`: normalização, SHA-256, deduplicação, detector
  de injeção (→ `EM_REVISAO`), divisão por seção com sobreposição (`DivisorTexto`), gravação em
  lotes, publicação em transação curta, desautorização da versão anterior e remoção de arquivos que
  saíram da pasta (FR-045).
- **Resiliência**: `ExecutorChamadaModelo` é o único ponto de novas tentativas (2, com backoff e
  variação aleatória, só em erro transitório); `TradutorExcecoesIa` classifica as exceções do SDK
  `openai-java`. Limite de requisições por conversa e total (`FiltroLimiteRequisicoes`,
  Resilience4j).
- **Swagger** nos dois agentes e **pgAdmin** no Docker Compose (pedidos do usuário).

## Decisões e o que foi descartado

| Decisão | Alternativa descartada | Por quê |
|---|---|---|
| Busca feita pelo código, `ChatClient` do redator sem ferramentas | Busca como `@Tool` | O modelo não precisa decidir quando buscar; ferramenta aumentaria chamadas e superfície de injeção (spring-ai §1). |
| `maxCompletionTokens` nas opções por chamada | `maxTokens` | O Spring AI 2.0.2 separa os dois; os modelos atuais da OpenAI recusam `max_tokens`. |
| Tradutor sobre `com.openai.errors.*` | `TransientAiException` do spring-ai-retry | O módulo não está no classpath na 2.0.2; o SDK lança as próprias exceções. |
| Limite de 413 aplicado no caso de uso | `@Size(max = 1000)` no DTO | Bean Validation daria 400; o contrato pede 413 para tamanho. A validação continua no backend. |
| `revisaoHumana: aprovada` no front matter | Publicar direto ou deixar sempre em revisão | O documento de fornecedores precisa estar na base para exercitar o cenário 4 (trecho malicioso recuperado); o campo simula a revisão humana e mantém o detector ativo para os demais. |
| Documento em `ERRO_PROCESSAMENTO` ou `EM_REVISAO` é reprocessado | Tratar como duplicado pelo hash | Com a deduplicação pura, uma queda do provedor ou uma aprovação de revisor nunca seriam reprocessadas. Defeito achado na revisão e coberto por teste. |
| Testes de Postgres com `@DynamicPropertySource` | `@ServiceConnection` | Conectaria como superusuário e esconderia os erros de permissão dos papéis. |

## Score e limiar (conferido no fonte do PgVectorStore 2.0.2)

Com `COSINE_DISTANCE`, `score = 1 − (embedding <=> consulta)` e o filtro SQL é
`distância < 1 − limiar`. A distância do cosseno vai de 0 a 2, o score de −1 a 1. O limiar inicial
0,55 (distância < 0,45) e o `top-k` 4 **ainda não foram calibrados** com o modelo de embedding
real: isso é o T066.

## Verificação (2026-10-27)

| Verificação | Resultado |
|---|---|
| `./mvnw verify` (dois módulos) | **BUILD SUCCESS** — agente-financeiro: 156 unitários + 27 de integração; agente-riscos: 2 + 4. 0 falhas. |
| Testes de integração com Postgres real | Busca só devolve AUTORIZADO e do modelo de embedding atual; não autorizado nunca aparece; ingestão com falha no 2º lote não deixa trecho; reingestão ignorada; arquivo removido apaga documento e vetores; versão nova desautoriza a anterior; papéis com menor privilégio. |
| WireMock como OpenAI | 429, 500 e timeout: 3 requisições (1 + 2 tentativas) e depois 503; 401: 1 requisição; JSON inválido: saída inválida sem retry no executor; corpo sem `tools` e com `max_completion_tokens`. |
| Jars contra o Compose | Os dois sobem; Flyway aplicou a V2; `/v3/api-docs` e Swagger UI respondem 200 nos dois; pergunta com chave falsa → 503 ProblemDetail com `Retry-After` e registro de execução com o mesmo `traceId`. |
| pgAdmin | `127.0.0.1:5050`, login com as credenciais do `.env` conferido (senha errada volta ao login); servidor "FinanceCorp (agente_leitura)" importado. |

## Pendências

- **T066 — eval com modelo real**: `./mvnw -pl agente-financeiro verify -Peval` com
  `OPENAI_API_KEY` e `OPENAI_CHAT_MODEL` definidos. Os casos estão em `eval/recuperacao.jsonl` (10)
  e `eval/resposta-documental.jsonl` (8); o relatório vai para `eval/resultados/`. Calibrar
  limiar e top-k até recuperação ≥ 90% e recusa 100%, e registrar aqui.
- **Escolha do modelo de chat** (research D9): os modelos de raciocínio da OpenAI podem recusar
  `temperature: 0.0`; conferir no eval e ajustar a configuração se for o caso.
- **Tokens no registro de execução**: os campos `tokensEntrada`/`tokensSaida` existem, mas ainda
  saem nulos; preencher a partir dos metadados de uso da resposta do Spring AI (research D13).
- Cenário 5 de US01 (pergunta mista) fica na Fase 5, como planejado.
