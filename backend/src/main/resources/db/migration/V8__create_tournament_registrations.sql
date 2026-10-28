CREATE TABLE squadra.tournament_registrations (
    id UUID PRIMARY KEY,
    team_id UUID NOT NULL REFERENCES squadra.teams(id) ON DELETE RESTRICT,
    tournament_id UUID NOT NULL REFERENCES squadra.tournaments(id) ON DELETE RESTRICT,
    category_id UUID NOT NULL REFERENCES squadra.tournament_categories(id) ON DELETE RESTRICT,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT tournament_registrations_status_check CHECK (
        status IN ('DRAFT', 'WAITING_ATHLETES', 'WAITING_PAYMENT', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'CANCELLED')
    ),
    CONSTRAINT tournament_registrations_unique_status UNIQUE (category_id, team_id, status)
);

CREATE INDEX idx_tournament_registrations_category_created ON squadra.tournament_registrations(category_id, created_at);
CREATE INDEX idx_tournament_registrations_team_created ON squadra.tournament_registrations(team_id, created_at);
CREATE INDEX idx_tournament_registrations_tournament_category ON squadra.tournament_registrations(tournament_id, category_id);
