# Eval de recuperação

- modelo de embedding: text-embedding-3-small
- top-k: 4 | limiar: 0.55

| caso | tipo | resultado | posição | similaridade |
|---|---|---|---|---|
| R01 | positivo | acertou | 1 | 0,648 |
| R02 | positivo | acertou | 1 | 0,589 |
| R03 | positivo | acertou | 1 | 0,698 |
| R04 | positivo | acertou | 1 | 0,665 |
| R05 | positivo | acertou | 1 | 0,712 |
| R06 | positivo | acertou | 1 | 0,593 |
| R07 | positivo | acertou | 1 | 0,588 |
| R08 | positivo | acertou | 1 | 0,665 |
| R09 | positivo | acertou | 1 | 0,741 |
| R10 | positivo | acertou | 1 | 0,655 |
| N01 | negativo | recusou | - | 0,506 (maior sem limiar) |
| N02 | negativo | recusou | - | 0,235 (maior sem limiar) |
| N03 | negativo | recusou | - | 0,447 (maior sem limiar) |
| N04 | negativo | recusou | - | 0,478 (maior sem limiar) |

**Positivos: 100%** (mínima 90%) | **Negativos: 100%** (mínima 100%)

Margem do limiar 0.55: menor positivo acertado = 0,588, maior negativo = 0,506. Um limiar entre os dois separa os grupos.
