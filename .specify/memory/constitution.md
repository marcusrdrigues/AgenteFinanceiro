# Constituição do AgenteFinanceiro

Sistema didático para uma aula prática de arquitetura de IA destinada a desenvolvedores Java
seniores. O AgenteFinanceiro responde perguntas financeiras combinando consultas numéricas
determinísticas, consulta documental por RAG e colaboração com outros agentes via A2A.

## Princípios Fundamentais

### I. Stack Fixa: Java 21, Spring Boot, Spring AI e Maven

- O sistema MUST usar Java 21, Spring Boot e Spring AI.
- Dependências MUST ser gerenciadas com Maven. Versões de Spring Boot e Spring AI MUST vir de
  BOM e ficar fixadas no `pom.xml`; não se usa versão `SNAPSHOT` nem faixa aberta.
- Dependência nova MUST ser justificada (finalidade, manutenção ativa, licença) antes de entrar.
- Nome de API, classe ou propriedade do Spring AI MUST ser conferido na documentação da versão
  fixada antes do uso.

Racional: a aula compara decisões de arquitetura; uma stack fixa e reproduzível evita que a
diferença entre máquinas dos alunos vire o assunto. Spring AI muda rápido entre versões.

### II. Arquitetura em Camadas com Dependência para Dentro

- O código MUST se organizar em camadas com responsabilidade única: `domain` (regras e
  entidades financeiras), `application` (casos de uso e portas), `infrastructure` (adaptadores:
  modelo, banco vetorial, fontes de dados, cliente/servidor A2A) e `interfaces` (entrada HTTP/A2A,
  que só traduz o protocolo).
- A dependência MUST apontar para dentro: `domain` não conhece Spring, SDK de IA, banco nem HTTP.
- Provedor de modelo, banco vetorial, fonte de dados financeiros e agentes remotos MUST ficar
  atrás de uma porta declarada em `application`.
- O código MUST seguir SOLID: cada classe tem um motivo para mudar; abstração só onde há
  fronteira real ou segunda implementação.
- Nomes de domínio em português (`ConsultarSaldoService`, `Lancamento`); termos técnicos em
  inglês (`Service`, `Port`, `Adapter`, `Controller`).

Racional: com o modelo atrás de uma porta, as regras financeiras são testadas sem chamar LLM e
o provedor pode ser trocado sem tocar no domínio.

### III. Entrega Incremental e Estabilidade do que Foi Validado

- Cada funcionalidade MUST ser entregue em incrementos pequenos, cada um compilando, com testes
  passando e documentação atualizada.
- Funcionalidade já validada (critérios de aceite atendidos e registrados) MUST NOT ser alterada
  sem necessidade registrada na spec ou tarefa correspondente. Toda alteração dessas MUST manter
  os testes existentes passando ou justificar, por escrito, a mudança do teste.

Racional: em aula, cada etapa serve de base para a seguinte; quebrar uma etapa validada invalida
o material que os alunos já seguiram.

### IV. Dinheiro é BigDecimal e Regra Financeira Tem Teste (INEGOCIÁVEL)

- Valores monetários MUST usar `BigDecimal`. `double` e `float` MUST NOT representar dinheiro.
- Escala e `RoundingMode` MUST ser explícitos em toda divisão e arredondamento, definidos num
  único ponto do domínio (por exemplo, um objeto de valor `Dinheiro`).
- Comparação de valores MUST usar `compareTo`, não `equals`, salvo quando a escala também faz
  parte da igualdade pretendida.
- Toda regra financeira MUST ter teste automatizado, incluindo casos de borda: zero, valor
  negativo, arredondamento, limite de período e ausência de dado.

Racional: erro de ponto flutuante e arredondamento implícito são as falhas clássicas de sistema
financeiro, e só teste as pega antes do usuário.

### V. O Modelo Não Inventa Dados Financeiros (INEGOCIÁVEL)

- Todo número financeiro apresentado ao usuário MUST vir de uma consulta determinística (tool
  ou caso de uso) executada naquela requisição. O modelo redige; não calcula nem supõe valores.
- Consultas numéricas (saldo, extrato, totais, indicadores) e consultas documentais (políticas,
  regulamentos, explicações) MUST seguir caminhos separados: as numéricas por tools com consulta
  fechada; as documentais por RAG. Valor numérico MUST NOT ser extraído de chunk do RAG para
  responder pergunta numérica.
- A resposta MUST ser validada em código: números presentes na saída MUST corresponder a valores
  devolvidos pelas tools da requisição. Divergência é falha, não ajuste silencioso.
- Sem dado disponível, o sistema MUST dizer que não tem a informação.
- Esse comportamento MUST ser coberto por eval versionado no repositório, com casos de pergunta
  sem dado, pergunta ambígua e tentativa de induzir o modelo a estimar valores.

Racional: instrução no prompt não garante que o modelo não alucine; a garantia está na separação
dos caminhos e na validação da saída em código.

### VI. RAG com Fonte Rastreável

- Cada chunk indexado MUST guardar: documento de origem, quem enviou, data de ingestão, trecho
  ou posição no documento e modelo de embedding com versão.
- Resposta documental MUST citar as fontes recuperadas que a sustentam. Sem fonte relevante, o
  sistema MUST recusar responder em vez de completar com conhecimento do modelo.
- Vetores de modelos de embedding diferentes MUST NOT ser misturados no mesmo índice.
- Apagar um documento MUST apagar seus chunks, vetores e caches derivados.
- Conteúdo recuperado é dado, não instrução: MUST ficar delimitado no prompt e MUST NOT alterar
  as tools disponíveis nem a política do agente.
- Recuperação e resposta MUST ser medidas separadamente no eval (o trecho certo veio entre os k
  primeiros? a resposta é fiel e cita a fonte certa?).

Racional: sem rastreabilidade não há como auditar uma resposta, nem remover conteúdo errado ou
envenenado da base.

### VII. Somente Leitura, Sem Operação Bancária Real (INEGOCIÁVEL)

- Toda tool financeira MUST ser somente leitura. Nenhuma tool cria, altera ou apaga dado
  financeiro.
- O sistema MUST NOT executar operação bancária real (transferência, pagamento, PIX, investimento,
  resgate) nem se integrar a API de instituição financeira com permissão de escrita.
- Fontes de dados MUST usar credencial somente leitura e consultas fechadas com parâmetros
  vinculados; consulta montada pelo modelo MUST NOT ser executada.
- A autorização de cada tool MUST ser conferida em código, a cada chamada, com a identidade da
  sessão autenticada. O identificador de conta MUST vir da sessão, não do texto do modelo.
- Pedido de operação MUST ser recusado com mensagem clara, sem simular execução.

Racional: somente leitura reduz o pior caso de uma injeção de prompt a vazamento, não a perda
financeira; e o vazamento é contido pela autorização em código.

### VIII. Integração entre Agentes pelo Protocolo A2A

- A comunicação entre o AgenteFinanceiro e outros agentes MUST usar o protocolo A2A
  (Agent2Agent) conforme a especificação oficial: descoberta por Agent Card, mensagens e tarefas
  no formato do protocolo.
- Um endpoint REST próprio MUST NOT substituir o protocolo. REST pode existir para outros fins
  (por exemplo, a interface com o usuário), mas não como canal entre agentes.
- O adaptador A2A MUST ficar na camada `infrastructure`/`interfaces`, atrás de porta; o domínio
  não conhece o protocolo.
- Chamada A2A MUST ter autenticação entre agentes, timeout, retry só em erro transitório e limite
  de tentativas.
- Resposta de agente remoto MUST ser tratada como conteúdo externo não confiável: validada contra
  schema e nunca promovida a instrução.
- O contrato A2A MUST ter teste de contrato.

Racional: o objetivo didático é mostrar interoperabilidade padronizada entre agentes; uma API
REST ad hoc esconderia exatamente o que a aula quer ensinar.

### IX. Segredos Fora do Código e Logs Sem Dado Sensível

- Segredo (chave de API, senha, token, certificado) MUST NOT aparecer no código, em arquivo
  versionado, em teste nem em prompt.
- Credenciais MUST ser lidas de variáveis de ambiente. O repositório MUST ter `.env` no
  `.gitignore` e pode trazer um `.env.example` só com nomes de variáveis.
- Toda requisição MUST ter identificador de correlação que ligue entrada, chunks recuperados,
  chamadas ao modelo, chamadas de tool, chamadas A2A e resposta.
- Logs de execução MUST registrar metadados (operação, duração, tokens, tool chamada, resultado,
  erro). Prompt, resposta, valores financeiros e identificadores pessoais MUST NOT aparecer em
  claro; quando necessários, ficam mascarados.
- Resposta de erro MUST NOT expor stack trace, nome de classe ou detalhe interno.

Racional: log é o caminho mais comum de vazamento acidental, e segredo em repositório público é
explorado em minutos.

## Restrições de Segurança e Dados

- Os dados financeiros do sistema MUST ser fictícios ou sintéticos. Dado real de pessoa ou de
  conta MUST NOT entrar no repositório, na base vetorial nem no provedor de modelo.
- O agente MUST NOT reunir ao mesmo tempo acesso a dado privado, leitura de conteúdo não
  confiável e canal de saída para fora sem as defesas dos princípios V, VI, VII e VIII; qualquer
  funcionalidade que combine as três MUST justificar o desenho na spec.
- Toda chamada ao modelo MUST ter limite em código: tokens de saída, iterações do agente,
  chamadas de tool, timeout e limite de requisições por usuário.
- O modelo MUST ser fixado por identificador com versão em configuração; parâmetros (temperatura,
  tokens) ficam em configuração com o motivo do valor.
- Prompts MUST ficar em arquivos próprios versionados, com instrução, contexto recuperado e
  entrada do usuário separados por delimitadores.
- Em falha do provedor ou limite atingido, o usuário MUST receber mensagem clara, nunca resposta
  inventada.

## Fluxo de Desenvolvimento e Critérios de Pronto

- Toda funcionalidade MUST começar pela spec (Spec Kit: specify → plan → tasks) e só ir para
  código depois das tarefas aprovadas.
- Toda spec MUST ter critérios de aceite verificáveis. Spec de IA MUST responder também: que dados
  o modelo enxerga e para onde vão, que tools pode chamar, qual modelo e por quê, casos de eval e
  nota mínima, limites de consumo, comportamento em falha e o que vai para log.
- Uma funcionalidade só está concluída quando:
  1. os critérios de aceite estão atendidos e registrados;
  2. os testes automatizados (unidade, integração e contrato, conforme o caso) foram escritos e
     passam no build Maven;
  3. o comportamento do modelo, quando houver, passa no eval na nota mínima;
  4. a documentação em Markdown da etapa está atualizada.
- Testes de unidade e integração MUST NOT chamar o modelo real; o modelo entra como dublê pela
  porta. O modelo real só é exercitado pelo eval.
- Toda etapa (spec, plano, tarefas, decisões de arquitetura, roteiro de aula) MUST ser documentada
  em Markdown no repositório.

## Governança

- Esta constituição prevalece sobre outras práticas do projeto. Conflito entre um artefato (spec,
  plano, tarefa, código) e a constituição é resolvido a favor da constituição ou por emenda.
- Emenda MUST ser feita por alteração deste arquivo, com motivo, impacto nos artefatos existentes
  e plano de migração quando houver.
- Versionamento semântico: MAJOR para remoção ou redefinição incompatível de princípio; MINOR
  para princípio ou seção nova ou ampliação material; PATCH para esclarecimento e redação.
- Toda revisão de plano (`/speckit-plan`), análise (`/speckit-analyze`) e revisão de código MUST
  verificar a conformidade com os princípios. Exceção MUST ser registrada na seção de
  complexidade do plano, com justificativa e alternativa mais simples descartada.

**Version**: 1.0.0 | **Ratified**: 2026-10-27 | **Last Amended**: 2026-10-27
