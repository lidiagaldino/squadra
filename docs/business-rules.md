# Core Business Rules

## Atletas
- Perfil esportivo permanente.
- Pode participar de múltiplas equipes.
- Pode jogar por equipes diferentes em campeonatos distintos.
- Não pode representar duas equipes na mesma categoria.
- Pode disputar múltiplas categorias do mesmo campeonato somente se o regulamento permitir.
- Saída de equipe não apaga histórico.

## Equipes
- Não possuem capitã permanente.
- Podem existir sem campeonato ativo.
- Podem participar de vários campeonatos simultaneamente.
- Possuem owner/admins permanentes para gestão da entidade.

## Categorias
Cada categoria possui inscrições, pagamentos, limites de elenco/equipes, formato competitivo, jogos, classificação, premiações e agenda próprios.

## Inscrição
Representa `Equipe + Campeonato + Categoria`.

Status:
- `DRAFT`
- `WAITING_ATHLETES`
- `WAITING_PAYMENT`
- `UNDER_REVIEW`
- `APPROVED`
- `REJECTED`
- `CANCELLED`

Somente `APPROVED` participa de sorteio e competição.

## Elenco
Status comuns:
- `INVITED`
- `CONFIRMED`
- `DECLINED`
- `PENDING_PAYMENT`
- `APPROVED`
- `REMOVED`

## Auditoria
Devem ser auditáveis: aprovação/rejeição, pagamentos/reembolsos, sorteio, agenda, resultados, WO, mudanças de regulamento e overrides manuais.
