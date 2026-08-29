# Payments

## Modelos
Cada categoria usa exatamente um modelo:
- `TEAM`
- `ATHLETE`

Não usar ambos simultaneamente na mesma categoria no MVP.

## TEAM
Uma cobrança representa a inscrição da equipe.

## ATHLETE
Cada atleta gera sua própria cobrança. A organizadora pode exigir quantidade mínima de atletas pagantes para aprovação.

## Regras
- Valor final é calculado no backend.
- Frontend nunca confirma pagamento.
- Confirmação oficial vem do gateway/webhook.
- Webhooks são idempotentes.
- Reembolsos são registrados e auditados.
- Squadra não armazena dados brutos de cartão.
- Gateway inicial: Mercado Pago, atrás de uma abstração.
