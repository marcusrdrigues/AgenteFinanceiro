# AgenteFinanceiro

Assistente financeiro da **FinanceCorp**, uma empresa fictícia, construído para uma aula prática
de arquitetura de IA para desenvolvedores Java seniores. Java 21, Spring Boot 4.1, Spring AI 2.0 e
protocolo A2A entre dois agentes.

> **⚠️ Sem autenticação — apenas ambiente local, não apto para produção.**
> Os dados são fictícios, a API e os bancos escutam só em `127.0.0.1`, e a conversa é identificada
> apenas por um UUID v4. Levar o sistema a qualquer ambiente compartilhado exige antes autenticação
> e posse de conversa (spec, "Restrições da aula").

## Módulos

| Módulo | Porta | Papel |
|---|---|---|
| `agente-financeiro` | 8080 | API REST: políticas (RAG), números (ferramentas somente leitura), memória, análise |
| `agente-riscos` | 8081 | Agente especialista em risco por regras fixas, sem modelo de linguagem, via A2A |

## Endereços locais

| O quê | Endereço |
|---|---|
| API do agente-financeiro | `http://localhost:8080/api/v1/conversas/{conversaId}/perguntas` |
| Swagger do agente-financeiro | `http://localhost:8080/swagger-ui.html` (OpenAPI em `/v3/api-docs`) |
| Swagger do agente-riscos | `http://localhost:8081/swagger-ui.html` |
| pgAdmin | `http://localhost:5050` — login `PGADMIN_EMAIL` / `PGADMIN_PASSWORD` do `.env`; servidor pré-cadastrado como `agente_leitura` (só leitura; a senha é `POSTGRES_APP_PASSWORD`) |
| PostgreSQL + pgvector | `127.0.0.1:5432`, banco `financecorp` |
| MongoDB | `127.0.0.1:27017` |

## Pré-requisitos

- JDK 21
- Docker com Compose

## Subir a infraestrutura local

```bash
cp .env.example .env      # preencha as senhas locais; o .env nunca é versionado
docker compose up -d --wait
./mvnw verify             # testes unitários e de integração (Testcontainers); não chamam a OpenAI
```

Carregar as políticas fictícias na base vetorial (usa a OpenAI para os embeddings):

```bash
cd agente-financeiro
OPENAI_API_KEY=... OPENAI_CHAT_MODEL=... ../mvnw spring-boot:run -Dspring-boot.run.profiles=ingestao
```

Para rodar os agentes, a validação completa e o eval, siga o
[quickstart](specs/001-assistente-financeiro/quickstart.md).

## Documentação

- Constituição: [.specify/memory/constitution.md](.specify/memory/constitution.md)
- Spec, plano e tarefas: [specs/001-assistente-financeiro/](specs/001-assistente-financeiro/)
- Decisões e registro de cada incremento: [docs/](docs/)
