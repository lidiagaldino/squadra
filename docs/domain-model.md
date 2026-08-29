# Domain Model Summary

## Entidades principais

### Identity
User, AthleteProfile, OrganizerProfile

### Teams
Team, TeamMembership

### Tournaments
Tournament, TournamentCategory, TournamentRegistration, RegistrationRoster, RegistrationStaff, Invitation

### Payments
Payment, Refund, PaymentWebhookEvent

### Competition
TournamentStage, TournamentGroup, GroupTeam, StageRound, Match, MatchEvent, Standing

### Scheduling
Venue, Court, ScheduleSlot, SchedulingConfig

### Gamification
AthleteStats, Badge, AthleteBadge, TeamBadge

### Platform
Notification, AuditLog

## Relações centrais

```text
Tournament
└── Category
    ├── Registrations
    │   ├── Team
    │   ├── Roster → Athlete
    │   └── Staff → User
    ├── Stages
    │   ├── Rounds
    │   ├── Matches
    │   └── Standings
    └── SchedulingConfig

Venue
└── Court
    └── ScheduleSlot
```

## Invariantes
- Uma equipe só pode ter uma inscrição por categoria.
- Uma atleta só pode representar uma equipe por categoria.
- Capitã pertence à inscrição, não à equipe.
- Team owner/admin pertence à equipe.
- Classificação oficial é calculada no backend.
- Pagamento oficial é confirmado pelo backend/gateway.
