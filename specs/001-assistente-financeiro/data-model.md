# Modelo de Dados: Assistente Financeiro da FinanceCorp

**Spec**: [spec.md](spec.md) | **Plano**: [plan.md](plan.md)

Convenção: nomes de domínio em português; termos técnicos em inglês. Objetos de valor e DTOs são
`record`. Valores monetários são sempre `BigDecimal` (constituição, princípio IV).

## 1. agente-financeiro

### 1.1 Domínio (sem Spring, sem Spring AI)

| Tipo | Forma | Campos | Regras |
|---|---|---|---|
| `Dinheiro` | record | `BigDecimal valor` | Escala 2, `RoundingMode.HALF_EVEN`, aplicados no construtor compacto; `null` recusado. Operações `somar`, `subtrair`, `negativo()`. Igualdade por `compareTo` (escala normalizada no construtor). `formatarBrl()` → `R$ 1.234,56` / `-R$ 12.500,00`; `paraDecimalTexto()` → `"1234.56"` (FR-005). |
| `Percentual` | record | `BigDecimal razao` | Escala 4, `RoundingMode.HALF_EVEN` no construtor compacto (único ponto de escala para razões, constituição IV). Fábrica `razao(Dinheiro parte, Dinheiro todo)` → `Optional.empty()` quando `todo` é zero (nunca divide por zero). `formatar()` → `46,9%` (1 casa). |
| `Periodo` | record | `LocalDate inicio`, `LocalDate fim` | `inicio <= fim`, ambos inclusivos; máximo 366 dias por consulta (limite de consumo). Fábricas: `mes(YearMonth)`, `trimestre(int ano, int n)`, `ano(int)`, `intervalo(inicio, fim)` (FR-014, FR-016). |
| `TipoLancamento` | enum | `RECEITA`, `DESPESA` | — |
| `Categoria` | enum | `VENDAS`, `SERVICOS`, `PESSOAL`, `INFRAESTRUTURA`, `MARKETING`, `IMPOSTOS`, `VIAGENS`, `OUTRAS` | Lista fechada; categoria desconhecida → `CategoriaInvalidaException`. |
| `Lancamento` | record | `UUID id`, `LocalDate data`, `TipoLancamento tipo`, `Categoria categoria`, `String descricao`, `Dinheiro valor` | `valor > 0`; o sinal vem do tipo. |
| `TotalCategoria` | record | `Categoria categoria`, `TipoLancamento tipo`, `Dinheiro total` | — |
| `ResumoFinanceiro` | record | `Periodo periodo`, `Periodo cobertura` (nullable), `Dinheiro receitas`, `Dinheiro despesas`, `Dinheiro resultado`, `List<TotalCategoria> porCategoria`, `boolean temDados` | `resultado = receitas − despesas`. `cobertura` é o intervalo efetivamente coberto por lançamentos (edge case "período parcial"). `temDados=false` → nenhum valor é apresentado (FR-008). |
| `NivelRisco` | enum | `BAIXO`, `MODERADO`, `ALTO`, `INDETERMINADO` | `INDETERMINADO` quando faltam receitas ou despesas (US04 cenário 6). |
| `IndicadorRisco` | record | `String codigo`, `String descricao`, `BigDecimal valor`, `boolean disparado` | Ex.: `RESULTADO_NEGATIVO`, `DESPESA_SOBRE_RECEITA`, `QUEDA_RECEITA`. |
| `AvaliacaoRiscoPorRegras` | record | `NivelRisco nivel`, `List<IndicadorRisco> indicadores` | Produzida por `AvaliadorRiscoPorRegras` (FR-022). |
| `TipoPergunta` | enum | `NUMERICA`, `DOCUMENTAL`, `MISTA`, `ANALISE`, `RISCO_ESPECIALIZADO`, `FORA_DO_DOMINIO` | Lista fechada de FR-039 (saída do classificador). |
| `TipoResposta` | enum | os seis valores de `TipoPergunta` + `NAO_INTERPRETADA` | Tipo devolvido na resposta da API; `NAO_INTERPRETADA` só quando a classificação falha (FR-041). |
| `ConversaId` | record | `UUID valor` | Aceita só UUID versão 4 (`valor.version() == 4`), senão `ConversaIdInvalidoException` (FR-002). |

**Critérios de risco iniciais (FR-022)**, em `AvaliadorRiscoPorRegras`, com limites em
`@ConfigurationProperties` (`financeiro.risco.*`) e documentados em `docs/criterios-risco.md`:

| Indicador | Dispara quando | Peso |
|---|---|---|
| `RESULTADO_NEGATIVO` | `resultado < 0` | decide `ALTO` sozinho |
| `DESPESA_SOBRE_RECEITA` | `Percentual.razao(despesas, receitas) > 0,90` | +1 |
| `QUEDA_RECEITA` | receita do período < 85% da receita do período anterior de mesma duração | +1 |

Nível: `ALTO` se `RESULTADO_NEGATIVO` ou 2 indicadores; `MODERADO` com 1; `BAIXO` com 0;
`INDETERMINADO` se `receitas` ou `despesas` sem dados.

### 1.2 Aplicação (records de entrada e saída dos casos de uso)

| Tipo | Campos | Uso |
|---|---|---|
| `ClassificacaoPergunta` | `TipoPergunta tipo`, `String perguntaAutonoma`, `Periodo periodo` (nullable), `Categoria categoria` (nullable) | Saída validada do classificador (FR-039). `perguntaAutonoma` é a pergunta reescrita sem depender do histórico (US03). |
| `ConsultaRegistrada` | `String ferramenta`, `Periodo periodo`, `Categoria categoria`, `ResumoFinanceiro resultado` | Cada chamada de ferramenta numérica feita na requisição. |
| `RegistroConsultas` | lista de `ConsultaRegistrada` | Escopo de **uma** requisição. Fonte única dos valores permitidos na resposta (FR-007). |
| `TrechoDocumento` | `UUID documentoId`, `String titulo`, `String versao`, `String secao`, `String texto`, `Double similaridade` | Trecho recuperado acima do limiar (FR-019, FR-020). |
| `Fonte` | `TipoFonte tipo` (`REGISTROS_FINANCEIROS`, `DOCUMENTO`, `AGENTE_ESPECIALISTA`, `REGRAS_INTERNAS`), `String titulo`, `String versao`, `String secao` | Derivada em código, nunca do texto do modelo. |
| `RespostaRedigida` | `String texto` | Texto redigido pelo modelo no caminho numérico, antes da verificação de valores. |
| `RespostaDocumentalRedigida` | `String texto`, `boolean conhecimentoSuficiente`, `List<String> referencias` (`T1..Tn`) | Retorno de `RedatorDocumentalPort`; o adaptador converte o record de saída do modelo (`RespostaDocumentalModelo`, em `infraestrutura.ia`) neste tipo. |
| `AnaliseRedigida` | `String resumo`, `List<RecomendacaoRedigida(texto, referencias)>` | Retorno de `RedatorAnalisePort`; convertido a partir de `AnaliseModelo` (em `infraestrutura.ia`). |
| `RegistroExecucao` | `String traceId`, `UUID conversaId`, `TipoResposta tipo`, `long duracaoMs`, `Integer tokensEntrada`, `Integer tokensSaida`, `Map<String,String> versoesPrompt`, `List<String> documentosConsultados` (`titulo@versao`), `List<String> consultasNumericas` (`ferramenta:inicio..fim:categoria`, sem valores), `UUID solicitacaoEspecialistaId` (nullable), `String resultadoEspecialista` (nullable), `String resultado`, `String codigoErro` (nullable) | Metadados de uma requisição (FR-036, FR-037). Nunca contém texto de pergunta, resposta ou valores monetários. |
| `RespostaAssistente` | `TipoResposta tipo`, `String texto`, `List<ValorMonetario> valores`, `Periodo periodo`, `List<Fonte> fontes`, `List<String> ausencias`, `AnaliseFinanceira analise` (nullable), `AvaliacaoRisco avaliacaoRisco` (nullable) | Resultado do caso de uso `ResponderPerguntaService`; vira o DTO HTTP (FR-042). |
| `ValorMonetario` | `String rotulo` (ex.: `receitas`), `Dinheiro valor` | — |
| `AnaliseFinanceira` | `String resumo`, `ResumoFinanceiro numeros`, `AvaliacaoRiscoPorRegras risco`, `List<Recomendacao> recomendacoes` | Seis seções de FR-023. **Números e risco preenchidos pelo código**; o modelo só redige `resumo` e `recomendacoes`. |
| `Recomendacao` | `String texto`, `List<Fonte> fontes` | FR-025. |
| `AvaliacaoRisco` | `OrigemAvaliacao origem` (`AGENTE_ESPECIALISTA`, `REGRAS_INTERNAS`), `NivelRisco nivel`, `String justificativa`, `boolean especialistaDisponivel` | FR-034, FR-035. |

### 1.3 Persistência

**PostgreSQL** (banco `financecorp`), esquema criado só por migração Flyway.

`financeiro.lancamento`

| Coluna | Tipo | Restrição |
|---|---|---|
| `id` | `uuid` | PK |
| `data` | `date` | not null, índice |
| `tipo` | `varchar(10)` | `CHECK (tipo IN ('RECEITA','DESPESA'))` |
| `categoria` | `varchar(20)` | not null, `CHECK` com a lista do enum |
| `descricao` | `varchar(200)` | not null |
| `valor` | `numeric(15,2)` | `CHECK (valor > 0)` |

- Dados fictícios em migração própria (`V2__lancamentos_ficticios.sql`): jan–dez/2025, com meses de
  déficit (ex.: ago/2025) e queda de receita (ex.: out/2025) para exercitar os níveis de risco.
- Papéis (criados em `infra/postgres/init/01-papeis.sh`):
  - `agente_leitura`: `SELECT` apenas em `financeiro.lancamento` e `conhecimento.*`; é o datasource
    da aplicação (constituição VII).
  - `agente_ingestao`: `SELECT, INSERT, UPDATE, DELETE` só em `conhecimento.*`, nenhum privilégio
    em `financeiro`; usado só pelo perfil `ingestao`.
  - `agente_migracao`: dono dos schemas, usado só pelo Flyway.

`conhecimento.documento`

| Coluna | Tipo | Restrição |
|---|---|---|
| `id` | `uuid` | PK |
| `titulo` | `varchar(200)` | not null |
| `versao` | `varchar(20)` | not null; único com `titulo` |
| `vigente_desde` | `date` | not null |
| `situacao` | `varchar(20)` | `CHECK` em `PROCESSANDO`, `AUTORIZADO`, `NAO_AUTORIZADO`, `EM_REVISAO`, `ERRO_PROCESSAMENTO` |
| `arquivo_origem` | `varchar(200)` | not null (nome do arquivo no conjunto curado) |
| `cadastrado_por` | `varchar(100)` | not null (quem enviou; na carga administrativa, `ingestao:<usuario-do-sistema>`) |
| `hash_conteudo` | `char(64)` | único (SHA-256 do texto normalizado) |
| `texto` | `text` | guardado para reindexação |
| `quantidade_trechos` | `int` | — |
| `modelo_embedding` | `varchar(60)` | not null |
| `cadastrado_em` | `timestamptz` | not null |

`conhecimento.vector_store` (formato exigido pelo `PgVectorStore`; colunas conferidas na versão
fixada, ver [research.md](research.md)): `id uuid`, `content text`, `metadata json/jsonb`,
`embedding vector(1536)`, índice HNSW com distância cosseno.

Metadados obrigatórios de cada trecho: `documentoId`, `titulo`, `versao`, `secao`, `ordem`,
`situacao`, `modeloEmbedding`, `arquivoOrigem`, `cadastradoPor`, `cadastradoEm` (constituição VI). Sem `donoId`: base única para todos (clarificação
P1: A); a busca filtra por `situacao == 'AUTORIZADO'` e `modeloEmbedding == <atual>` **dentro** da
busca vetorial.

Transições de `situacao`: `PROCESSANDO → AUTORIZADO | EM_REVISAO | ERRO_PROCESSAMENTO`;
`AUTORIZADO → NAO_AUTORIZADO` por processo administrativo ou quando chega versão nova do mesmo
título (o metadado `situacao` dos trechos é atualizado e eles deixam de aparecer na busca).
Documento cujo arquivo saiu do conjunto curado é **apagado** com seus trechos e vetores (FR-045).

**MongoDB** (banco `financecorp_memoria`): coleção do repositório de memória de chat do Spring AI
(nome e formato do documento conferidos em [research.md](research.md)). Chave: `conversaId`
(UUID v4 como texto). Índice TTL de 30 dias sobre o campo de data da mensagem (FR-029). Só entram a
pergunta do usuário e o texto final da resposta; não entram trechos de documentos, resultados de
ferramentas nem respostas intermediárias do classificador.

## 2. agente-riscos

Sem banco de dados e sem modelo de linguagem (clarificação P2: C).

| Tipo | Forma | Campos | Regras |
|---|---|---|---|
| `Dinheiro` | record | `BigDecimal valor` | Mesmas regras de escala do agente-financeiro (escala 2, HALF_EVEN); cada módulo tem o seu, sem código compartilhado. |
| `Percentual` | record | `BigDecimal razao` | Escala 4, HALF_EVEN; `razao(parte, todo)` → vazio quando `todo` é zero. Único ponto de escala para razões no módulo. |
| `IndicadoresFinanceiros` | record | `Periodo periodo`, `Dinheiro receitas`, `Dinheiro despesas`, `Dinheiro resultado`, `Map<String, Dinheiro> despesasPorCategoria`, `Dinheiro receitasPeriodoAnterior` (nullable) | `resultado == receitas − despesas` conferido na entrada; divergência → solicitação rejeitada. Receitas e despesas ambas zero → rejeitada (FR-046). |
| `RegraRisco` | interface | `Optional<Achado> avaliar(IndicadoresFinanceiros)` | Padrão Strategy: cada regra uma classe. |
| `Achado` | record | `String codigo`, `String descricao`, `int pontos` | — |
| `ParecerRisco` | record | `NivelRisco nivel`, `int pontuacao`, `List<Achado> achados`, `String justificativa` | `justificativa` montada por modelo de texto fixo com os achados. |

Regras do especialista (mais ricas que as internas do agente-financeiro, para que as duas origens
sejam distinguíveis em aula): margem líquida < 5% (não se aplica com receitas zero), concentração de uma categoria de despesa > 40%,
resultado negativo, queda de receita > 15%. Pontuação → nível: 0 `BAIXO`, 1–2 `MODERADO`, ≥ 3 ou
resultado negativo `ALTO`.

## 3. Mensagem trocada por A2A

Definida em [contracts/a2a-avaliacao-risco.md](contracts/a2a-avaliacao-risco.md). Cada lado tem
seus próprios `record`s espelhando o JSON Schema do contrato; não há módulo de código compartilhado.
