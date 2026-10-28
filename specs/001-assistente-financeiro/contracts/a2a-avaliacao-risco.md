# Contrato A2A: avaliação de risco (agente-financeiro → agente-riscos)

**Protocolo**: A2A v1.0, transporte JSON-RPC, SDK `org.a2aproject.sdk` 1.4.0.Final
(ver [../research.md](../research.md), D7). Este documento define o **conteúdo** trocado; o
envelope (mensagem, tarefa, artefato, estados) é o do protocolo e não é redefinido aqui.

## 1. Agent Card (servido pelo agente-riscos)

`GET http://localhost:8081/.well-known/agent-card.json`

Campos obrigatórios do nosso card (nomes de campo do protocolo conferidos no incremento 6):

| Campo | Valor |
|---|---|
| nome | `agente-riscos` |
| descrição | "Avalia risco financeiro por regras fixas a partir de indicadores agregados. Dados fictícios." |
| versão | `1.0.0` |
| URL do serviço | `http://localhost:8081` (caminho JSON-RPC conforme a autoconfiguração) |
| modos de entrada/saída | `application/json` |
| skill | id `avaliar-risco-financeiro`, nome "Avaliar risco financeiro", tags `financeiro`, `risco` |
| streaming | `false` |

| esquema de segurança | HTTP `bearer` (declarado no card; nomes de campo do protocolo conferidos no incremento 6) |

O Agent Card é público (sem credencial). O agente-financeiro obtém o card na primeira chamada, guarda-o em cache e confere `nome` e o id da
skill; card divergente → `EspecialistaIndisponivelException` (sem chamar).

## 1.1 Autenticação entre agentes (FR-044, constituição VIII)

- Toda requisição ao endpoint JSON-RPC MUST trazer `Authorization: Bearer <token>`, com o token
  lido de `A2A_TOKEN_RISCOS` nos dois agentes (mínimo 32 caracteres; a subida falha se ausente ou
  curto, citando o nome da variável).
- O agente-riscos compara o token em tempo constante (`MessageDigest.isEqual`) e responde **HTTP
  401** sem corpo de detalhe quando falta ou diverge, antes de qualquer processamento.
- O token nunca aparece em log, Agent Card, resposta ou exemplo versionado.
- No agente-financeiro, 401 é **não transitório**: sem nova tentativa, fallback de FR-034 e log
  de configuração (sem o token).
- Alternativa descartada: mTLS — mais forte, mas exige gerenciar certificados locais, o que
  desvia o foco da aula; fica registrado como o caminho para qualquer ambiente compartilhado.

## 2. Mensagem de solicitação

O agente-financeiro **não** envia solicitação para período sem dados (FR-046).

Uma mensagem com papel de usuário contendo **uma** parte de dados (JSON) com:

```json
{
  "esquema": "financecorp.risco.solicitacao/v1",
  "solicitacaoId": "3f0b8a3e-6a1c-4c7e-9a52-0c8f2d4b9e11",
  "periodo": { "inicio": "2025-01-01", "fim": "2025-03-31" },
  "moeda": "BRL",
  "receitas": "400000.00",
  "despesas": "320000.00",
  "resultado": "80000.00",
  "despesasPorCategoria": {
    "PESSOAL": "150000.00",
    "INFRAESTRUTURA": "90000.00",
    "MARKETING": "50000.00",
    "IMPOSTOS": "30000.00"
  },
  "receitasPeriodoAnterior": "380000.00"
}
```

Regras (validadas pelo agente-riscos; violação → tarefa em estado de falha com mensagem
"solicitação inválida", sem detalhe interno):

- `esquema` exatamente `financecorp.risco.solicitacao/v1`.
- Valores como texto decimal `^-?\d+\.\d{2}$`; nunca número JSON.
- `resultado == receitas − despesas`; soma de `despesasPorCategoria == despesas`.
- `periodo.inicio <= periodo.fim`, no máximo 366 dias.
- `receitasPeriodoAnterior` opcional (nulo quando não há período anterior com dados).
- `receitas` e `despesas` ambos `"0.00"` → recusada (FR-046). Com `receitas == "0.00"` e despesas
  positivas, a regra de margem não se aplica (sem divisão por zero) e `RESULTADO_NEGATIVO` decide.
- Somente agregados: nenhum lançamento individual, descrição, dado de usuário ou `conversaId`
  (FR-031). `solicitacaoId` é um UUID novo por chamada, usado só para correlação.

## 3. Resposta

Tarefa concluída com **um** artefato contendo uma parte de dados:

```json
{
  "esquema": "financecorp.risco.parecer/v1",
  "solicitacaoId": "3f0b8a3e-6a1c-4c7e-9a52-0c8f2d4b9e11",
  "nivel": "MODERADO",
  "pontuacao": 1,
  "achados": [
    { "codigo": "CONCENTRACAO_DESPESA", "descricao": "PESSOAL representa 46,9% das despesas", "pontos": 1 }
  ],
  "justificativa": "Risco moderado: 1 achado. PESSOAL representa 46,9% das despesas.",
  "avaliadoPor": "agente-riscos/1.0.0"
}
```

Validação no agente-financeiro (FR-032, FR-033):

- `esquema` exatamente `financecorp.risco.parecer/v1`; `solicitacaoId` igual ao enviado.
- `nivel` ∈ {`BAIXO`, `MODERADO`, `ALTO`}; `achados` lista (pode ser vazia); `justificativa` até
  1.000 caracteres.
- Qualquer violação → resposta descartada, fallback de FR-034.
- O texto de `justificativa` e `descricao` é **dado não confiável**: é exibido como citação do
  especialista, nunca enviado ao modelo como instrução nem usado para acionar ação. Valores
  monetários apresentados ao usuário vêm sempre do `ResumoFinanceiro` local; divergência entre
  números citados pelo especialista e os enviados é registrada em log (só códigos e contagem).

## 4. Falhas e estados

| Situação | Comportamento no agente-financeiro |
|---|---|
| HTTP 401 (credencial ausente ou errada) | Fallback imediato, sem nova tentativa |
| Tarefa em estado de falha ou rejeitada | Fallback FR-034 (regras internas) |
| Tempo limite (5 s) por tentativa | 1 nova tentativa; depois fallback |
| Conexão recusada / 5xx | Transitório: 1 nova tentativa; depois fallback |
| Disjuntor aberto (5 falhas seguidas) | Fallback imediato por 30 s, sem chamar |
| Resposta fora do schema | Fallback, sem nova tentativa |

Em todos os casos a API do agente-financeiro responde 200 com `avaliacaoRisco.origem =
REGRAS_INTERNAS` e `especialistaDisponivel = false` (SC-007, SC-011).

## 5. Testes de contrato

- **agente-riscos**: sobe em porta aleatória; cliente A2A real do SDK busca o card, envia o
  exemplo da §2 e casos inválidos (esquema errado, `resultado` incoerente, número JSON) e confere
  a §3 e os estados de falha.
- **agente-financeiro**: adaptador A2A contra um servidor A2A falso em **WireMock**, que devolve o
  Agent Card e envelopes JSON-RPC **gravados de uma troca real** com o agente-riscos (spike do
  incremento 6, salvos em `contracts/exemplos/jsonrpc/`), com variantes: válida, fora do schema,
  lenta, 500, 401, fora do ar, `solicitacaoId` trocado, skill diferente. O WireMock também confere
  que o cabeçalho `Authorization` foi enviado.
- **Ligação com o rastreamento**: o agente-financeiro registra `traceId` e `solicitacaoId` juntos
  no registro de execução; o agente-riscos registra `solicitacaoId` (FR-037).
- Os exemplos JSON desta página ficam versionados em `src/test/resources/contrato-a2a/` dos dois
  módulos, iguais byte a byte; um teste em cada módulo falha se divergirem do arquivo de referência
  em `specs/001-assistente-financeiro/contracts/exemplos/`.
