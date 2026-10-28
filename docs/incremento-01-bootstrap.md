# Incremento 1 — Bootstrap e infraestrutura local

**Data**: 2026-10-27 | **Tarefas**: T001–T033 | **Status**: concluído

## O que foi montado

- **Maven multimódulo**: `pom.xml` raiz (parent Spring Boot 4.1.2, BOM Spring AI 2.0.2, versões
  centralizadas, Surefire para `*Test`, Failsafe para `*IT`, perfil `eval`) e dois módulos sem
  dependência entre si. Maven Wrapper 3.9.16.
- **Infraestrutura local** (`docker-compose.yml`): `pgvector/pgvector:0.8.7-pg18` e `mongo:8.0.34`,
  portas só em `127.0.0.1`, healthchecks, senhas do `.env`.
- **Papéis do banco** (`infra/postgres/init/01-papeis.sh`): `agente_migracao` (Flyway),
  `agente_ingestao` (só `conhecimento`), `agente_leitura` (datasource da aplicação, só leitura).
  Privilégios por schema na migração `V1__esquemas.sql`.
- **Domínio**: `Dinheiro` (escala 2, HALF_EVEN), `Percentual` (escala 4), `Periodo`,
  `ConversaId` (só UUID v4 canônico), enums e exceções de domínio.
- **Borda**: `ErroHandler` (ProblemDetail sem detalhe interno; `instance` é o traceId, não o
  caminho com o conversaId), `FiltroTraceId` (gerado no servidor, nunca aceito do cliente).
- **Configuração**: records `@Validated` (`financeiro.ia`, `rag`, `limites`, `risco`);
  `VerificadorConfiguracaoIa` para a subida antes dos beans sem `OPENAI_API_KEY` /
  `OPENAI_CHAT_MODEL`; log de prompt e resposta desligado nas observações do Spring AI.
- **Registro de execução**: `RegistroExecucao` + porta + adaptador SLF4J, uma linha JSON por
  requisição em `logs/agente-financeiro-execucao.log`, rotação diária, 90 dias.

## Decisões

- **Sem módulo compartilhado** entre os agentes: o contrato entre eles é o A2A, verificado por
  teste nos dois lados (research D7).
- **Bind em 127.0.0.1** na API e nos bancos: compensação da ausência de autenticação.
- **`@DynamicPropertySource` em vez de `@ServiceConnection`** para o Postgres nos testes: o
  `@ServiceConnection` conectaria a aplicação como superusuário do container e esconderia erros de
  permissão dos papéis. A alternativa descartada era mais curta, mas tornaria os testes de
  privilégio (T043, T077) inúteis.
- **Escopo do bootstrap**: entraram só as dependências de base e o starter OpenAI (necessário para
  configurar e verificar o provedor; nenhuma chamada ao modelo). pgvector, memória MongoDB,
  Resilience4j, WireMock e A2A entram nos incrementos deles. O Mongo já sobe no Compose, mas a
  aplicação ainda não se conecta a ele.
- **BOM do A2A não importado**: ver [decisoes/0001-dependencias.md](decisoes/0001-dependencias.md).

## Como subir

```bash
cp .env.example .env    # preencher senhas
docker compose up -d --wait
./mvnw verify
./mvnw -q -DskipTests package
OPENAI_API_KEY=... OPENAI_CHAT_MODEL=... java -jar agente-financeiro/target/agente-financeiro-0.1.0-SNAPSHOT.jar
java -jar agente-riscos/target/agente-riscos-0.1.0-SNAPSHOT.jar
```

## Resultado da verificação (2026-10-27)

| Verificação | Resultado |
|---|---|
| `./mvnw verify` | **BUILD SUCCESS** — 73 testes, 0 falhas: agente-financeiro 63 unitários + 5 de integração; agente-riscos 2 + 3 |
| `docker compose up -d --wait` | `postgres` e `mongo` `healthy`; portas `127.0.0.1:5432` e `127.0.0.1:27017` |
| Papéis e extensão | `agente_ingestao`, `agente_leitura`, `agente_migracao`; extensão `vector` |
| Subida sem `OPENAI_API_KEY` | Para com "Configuração obrigatória ausente: defina a variável de ambiente OPENAI_API_KEY ..." |
| Jar do agente-financeiro contra o Compose | `/actuator/health` `UP`; Flyway aplicou V1 no schema `financeiro` |
| Jar do agente-riscos | `/actuator/health` `UP`, sem nenhuma variável de modelo |
| Rota inexistente | 404 `application/problem+json`, com `X-Trace-Id`, sem ecoar o caminho |
| `git check-ignore` | `.env` e `logs/` ignorados; `.env.example` sem valores |

Primeira tentativa de subir o jar falhou (`NoClassDefFoundError: okhttp3/Callback`) com o `verify`
verde — causa e correção em [decisoes/0001-dependencias.md](decisoes/0001-dependencias.md).
