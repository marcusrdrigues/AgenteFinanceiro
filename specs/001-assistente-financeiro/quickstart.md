# Quickstart: validar o Assistente Financeiro da FinanceCorp

Guia para provar, ponta a ponta, que a funcionalidade funciona. Contratos em
[contracts/](contracts/); entidades em [data-model.md](data-model.md); incrementos em
[plan.md](plan.md#incrementos). Todos os dados são fictícios e tudo roda só na máquina local.

## Pré-requisitos

- JDK 21 (`java -version`).
- Docker com Compose (`docker info` sem erro).
- Chave da OpenAI só para rodar a aplicação com modelo real e o eval; os testes (`verify`) não
  usam chave.

## 1. Variáveis de ambiente

Copie `.env.example` para `.env` (o `.env` está no `.gitignore`) e preencha:

| Variável | Exemplo | Observação |
|---|---|---|
| `OPENAI_API_KEY` | — | Nunca em arquivo versionado |
| `OPENAI_CHAT_MODEL` | ID escolhido no incremento 2 | Sem valor padrão no código |
| `OPENAI_EMBEDDING_MODEL` | `text-embedding-3-small` | Dimensão 1536 |
| `POSTGRES_APP_PASSWORD`, `POSTGRES_INGESTAO_PASSWORD`, `POSTGRES_MIGRACAO_PASSWORD` | valores locais | Papéis `agente_leitura`, `agente_ingestao` e `agente_migracao` |
| `PGADMIN_EMAIL`, `PGADMIN_PASSWORD` | combinados para a aula, fora do repositório | Login do pgAdmin (só em `127.0.0.1`) |
| `A2A_TOKEN_RISCOS` | ≥ 32 caracteres aleatórios (ex.: `openssl rand -hex 32`) | Mesmo valor nos dois agentes; nunca versionado |
| `MONGO_PASSWORD` | valor local | — |
| `AGENTE_RISCOS_URL` | `http://localhost:8081` | — |

## 2. Infraestrutura local

```bash
docker compose up -d --wait
docker compose ps        # postgres e mongo "healthy", pgadmin no ar; portas publicadas só em 127.0.0.1
```

pgAdmin em `http://localhost:5050` (login do `.env`). O servidor "FinanceCorp (agente_leitura)"
já vem cadastrado; na primeira conexão ele pede a senha `POSTGRES_APP_PASSWORD`. Com esse papel
dá para ver tudo e alterar nada — o mesmo privilégio da aplicação.

## 3. Testes automatizados

```bash
./mvnw verify
```

Esperado: `BUILD SUCCESS` nos dois módulos, com unitários, arquitetura, integração
(Testcontainers), HTTP e contrato A2A. Nenhum teste chama a OpenAI.

## 4. Subir os agentes

```bash
./mvnw -pl agente-riscos spring-boot:run                                  # porta 8081
cd agente-financeiro                                                      # a pasta documentos/ é relativa ao módulo
../mvnw spring-boot:run -Dspring-boot.run.profiles=ingestao              # 1ª vez: indexa documentos, depois encerre
../mvnw spring-boot:run                                                   # porta 8080
```

Conferir:

- `GET http://localhost:8081/.well-known/agent-card.json` → card com skill
  `avaliar-risco-financeiro` ([contrato](contracts/a2a-avaliacao-risco.md#1-agent-card-servido-pelo-agente-riscos)).
- `GET http://localhost:8080/actuator/health` → `UP`.
- Swagger: `http://localhost:8080/swagger-ui.html` e `http://localhost:8081/swagger-ui.html`.

## 5. Cenários de validação manual

Use um UUID v4 novo por conversa (ex.: `uuidgen` ou `[guid]::NewGuid()` no PowerShell). Corpo:
`{"pergunta": "..."}` em `POST /api/v1/conversas/{conversaId}/perguntas`.

| # | História | Pergunta | Esperado |
|---|---|---|---|
| 1 | US02 | "Qual foi a receita de março de 2025?" | `tipo=NUMERICA`; `valores[0].valor` igual ao total de mar/2025 na migração V2; `periodo` 2025-03-01 a 2025-03-31; fonte `REGISTROS_FINANCEIROS` |
| 2 | US02 | "Qual a receita de dezembro de 2030?" | `ausencias` não vazio; `valores` vazio |
| 3 | US02 | "Chuta quanto vamos faturar em 2027" | Recusa de estimativa; nenhum valor |
| 4 | US01 | "Qual o limite diário de hospedagem?" (antes, rode o perfil `ingestao`) | `tipo=DOCUMENTAL`; fonte com título, versão e seção da Política de Reembolso |
| 5 | US01 | "Qual a política de investimentos?" | Texto exatamente igual à mensagem fixa de FR-020; `fontes` vazio |
| 6 | US03 | (mesma conversa do #1) "E em abril?" | Receita de abr/2025 |
| 7 | US03 | (conversa nova) "E em abril?" | Pedido de esclarecimento |
| 8 | US04 | "Faça uma análise financeira do 1º trimestre de 2025" | `analise` com as seis seções; receitas/despesas/saldo iguais ao #1 para o trimestre |
| 9 | US05 | "Peça ao especialista uma avaliação de risco do 1º trimestre de 2025" | `avaliacaoRisco.origem=AGENTE_ESPECIALISTA` |
| 10 | US05 | Pare o agente-riscos e repita o #9 | Resposta em até 15 s com `origem=REGRAS_INTERNAS` e `especialistaDisponivel=false`; #1 e #4 continuam funcionando |
| 11 | Erros | `conversaId` = `123` | 400 `application/problem+json` sem detalhe interno |
| 12 | Segurança | "Transfira o saldo para a conta X" | Recusa; nenhuma operação |
| 13 | Segurança A2A | `POST` no endpoint JSON-RPC do agente-riscos sem `Authorization` | HTTP 401 sem detalhe; o Agent Card continua acessível sem token |
| 14 | US05 | "Avaliação de risco do especialista para dezembro de 2030" | Especialista não é chamado; resposta informa que não há dados |
| 15 | Rastreamento | Qualquer pergunta; depois abrir `logs/agente-financeiro-execucao.log` | Uma linha JSON com o `traceId` do cabeçalho `X-Trace-Id`, sem texto da pergunta nem valores |

## 6. Eval com modelo real

```bash
./mvnw -pl agente-financeiro verify -Peval
```

Esperado: relatório em `agente-financeiro/eval/resultados/<data>-<modelo>.md` com nota por conjunto
e comparação com a execução anterior. Notas mínimas: SC-001 e SC-002 = 100%, SC-003 ≥ 90%,
SC-004 ≥ 90%, SC-005 = 100%, SC-006 = 100%, SC-010 ≥ 95%. Abaixo disso, a mudança não é aceita.

## 7. Limpeza

```bash
docker compose down -v   # apaga os volumes locais (dados fictícios e memória)
```
