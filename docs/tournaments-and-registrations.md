# Tournaments and Registrations

## Tournament
Evento principal com nome, descrição, cidade/estado, local, datas, prazo de inscrição, regulamento e status.

Status:
- `DRAFT`
- `OPEN`
- `CLOSED`
- `IN_PROGRESS`
- `FINISHED`
- `CANCELLED`

## Tournament Category
Divisão independente, como Iniciante ou Intermediário.

Configura:
- modalidade;
- limites de atletas;
- máximo de equipes;
- modelo de pagamento;
- valor;
- formato;
- regras específicas;
- agenda.

## Fluxo de inscrição
1. Equipe escolhe categoria.
2. TEAM_OWNER ou usuário autorizado inicia inscrição.
3. Define pelo menos um TEAM_MANAGER.
4. Configura staff temporário.
5. Convida atletas.
6. Atletas aceitam/recusam.
7. Pagamentos são concluídos.
8. Organizadora revisa.
9. Inscrição é aprovada/rejeitada.
10. Apenas aprovadas entram no Competition Engine.

A participação esportiva da atleta é contextual à inscrição.

## Visibilidade e convites

A organizadora escolhe `PUBLIC` (público) ou `PRIVATE` (privado) na criação
ou edição de um rascunho. O padrão é público, inclusive para torneios
anteriores à migração. A visibilidade não muda depois da publicação.
Mesmo os torneios públicos exigem login.

Em **Torneios**, usuários autenticados encontram os eventos públicos abertos.
Uma dona ou administradora de time pode escolher uma categoria e solicitar
inscrição. A inscrição começa em `DRAFT`; elenco, pagamento e aprovação
continuam sendo etapas posteriores.

Em torneios privados abertos, somente a organizadora proprietária pode
buscar equipes e enviar convites por categoria. A busca expõe apenas ID,
nome, cidade e estado. Convites pendentes não reservam vagas.

A seção **Convites para torneios**, no time, permite que `TEAM_OWNER` ou
`TEAM_ADMIN` aceite ou recuse um convite pendente:

- Aceitar muda o convite para `ACCEPTED` e cria uma inscrição `DRAFT` na
  mesma transação. Categoria cheia ou torneio fechado mantém o convite
  pendente e retorna conflito.
- Recusar muda o convite para `REJECTED`, sem criar inscrição.
- A organizadora pode cancelar um convite pendente (`CANCELLED`).
- Convites decididos permanecem no histórico. Uma nova tentativa de decisão
  retorna conflito; uma nova convocação após recusa ou cancelamento é permitida.

Somente a organizadora e administradoras de equipes com convite pendente ou
aceito, ou com inscrição ativa, podem consultar o evento privado. Outros
usuários recebem `404`. A API
revalida a administração da equipe em cada decisão.

Uma categoria admite somente uma inscrição ativa por equipe. Solicitações e
aceites usam bloqueio de categoria e unicidade no banco para proteger a última
vaga, inclusive sob concorrência.
