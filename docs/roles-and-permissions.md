# Roles and Permissions

## Papéis globais
- `ADMIN`
- `ORGANIZER`
- `ATHLETE`

## Papéis permanentes de equipe
### TEAM_OWNER
Administra a entidade equipe, pode editar dados permanentes, transferir propriedade e iniciar inscrições.

### TEAM_ADMIN
Auxilia na administração permanente da equipe.

### MEMBER
Atleta vinculada à equipe.

## Papéis temporários por inscrição
- `TEAM_MANAGER`
- `CAPTAIN`
- `COACH`
- `ASSISTANT_COACH`

CAPTAIN não é papel permanente da equipe.

## Regra de autorização
Toda permissão contextual deve validar:
`authenticated user + role + resource + context`
