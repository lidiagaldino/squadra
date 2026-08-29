# Security and Business Integrity Constraints

## Autorização
- Toda regra de acesso é validada no backend.
- IDs recebidos do cliente não provam autorização.
- Organizadora só gerencia campeonatos autorizados.
- TEAM_MANAGER/CAPTAIN são validados no contexto da inscrição.
- TEAM_OWNER/TEAM_ADMIN são validados no contexto da equipe.

## Integridade financeira
- Valor é calculado no backend.
- Frontend não define `PAID`.
- Webhooks são autenticados e idempotentes.
- Reembolso exige autorização e auditoria.

## Integridade competitiva
Somente usuários autorizados podem confirmar sorteio, alterar classificação oficial, registrar/alterar resultado, aplicar WO e publicar/reagendar agenda.

Toda alteração oficial deve ser auditável.

## Privacidade
- Campeonatos não são públicos sem login no MVP.
- Não expor dados pessoais desnecessariamente.
- Coletar apenas dados necessários.

## Badges
- Nunca atribuídos pelo cliente.
- Concessão ocorre no backend e é idempotente.

## Logs
Nunca registrar senha, tokens, secrets ou dados financeiros sensíveis.
