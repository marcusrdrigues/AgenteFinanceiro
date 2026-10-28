# 0002 — Modelo de chat: gpt-6-luna

**Data**: 2026-10-28 | **Status**: decidido pelo usuário | **Tarefa**: T093 (adiantada do incremento 4)

## Decisão

`OPENAI_CHAT_MODEL=gpt-6-luna` para classificação e redação, com `reasoning-effort: none`.
Critério do usuário: projeto de estudo, o modelo mais barato que passar no eval.

| Item | Valor (docs oficiais, conferido em 2026-10-28) |
|---|---|
| Preço por 1M tokens | entrada US$ 0,10; saída US$ 0,50; entrada em cache US$ 0,01 |
| Contexto / saída máxima | 1.050.000 / 128.000 tokens |
| Tipo | modelo de raciocínio; `reasoning.effort` em `none`, `low`, `medium` (padrão), `high`, `xhigh`, `max` |
| Endpoints | Chat Completions, Responses, Batch |
| Function calling | Responses API; no Chat Completions **só com `reasoning_effort: none`** |
| Snapshot datado | não existe (exceção registrada no plano, Acompanhamento de Complexidade) |

Fonte: developers.openai.com/api/docs/models/gpt-6-luna.

## Teste rápido na API (Chat Completions, dados fictícios)

| Teste | Resultado |
|---|---|
| `temperature: 0` + `reasoning_effort: none` + `response_format` json_schema strict | aceito; JSON válido; 0 tokens de raciocínio |
| sem `reasoning_effort` (padrão `medium`), `max_completion_tokens: 50` | aceito; 18 dos 36 tokens de saída foram de raciocínio |
| `tools` + `reasoning_effort: none` | `finish_reason: tool_calls`, argumentos em JSON; 0 tokens de raciocínio |

## Consequências

- `spring.ai.openai.chat.reasoning-effort: none` em `application.yml` (propriedade conferida no
  metadata do Spring AI 2.0.2). Sem isso, o raciocínio consome o teto de 200 tokens do classificador
  e a resposta pode vir vazia; e o caminho numérico do incremento 4 não conseguiria chamar
  ferramentas.
- `temperature: 0.0` mantido: o modelo aceita com `reasoning_effort: none`.
- O Spring AI 2.0.2 usa o Chat Completions (SDK `openai-java`), por isso a restrição de function
  calling vale para o projeto.
- Trocar de modelo exige rodar o eval inteiro de novo (constituição, "Restrições").
