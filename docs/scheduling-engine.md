# Scheduling Engine

## Objetivo
Alocar partidas em data, horário e quadra respeitando restrições esportivas e operacionais.

## Entradas
Partidas, locais, quadras, slots, duração, intervalo, descanso mínimo e estratégia.

## Estratégias
- `FASTEST_COMPLETION`
- `BALANCED_REST`
- `BALANCED_COURTS`
- `HYBRID`
- `MANUAL`

## Restrições obrigatórias
Nunca permitir:
- dois jogos simultâneos da mesma equipe;
- dois jogos simultâneos na mesma quadra.

Para publicação, toda partida deve possuir horário e quadra.

## Otimização
Tentar:
- respeitar descanso mínimo;
- equilibrar descanso;
- distribuir quadras;
- evitar jogos consecutivos;
- reduzir esperas excessivas;
- equilibrar primeiros/últimos horários;
- respeitar dependências entre fases;
- posicionar finais após semifinais.

## Edição manual
Organizadora pode trocar horário/quadra, reagendar, bloquear slot e recalcular. Toda alteração deve ser revalidada e auditada.
