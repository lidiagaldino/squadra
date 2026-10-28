CREATE TABLE squadra.tournament_invitations (
    id UUID PRIMARY KEY,
    tournament_id UUID NOT NULL REFERENCES squadra.tournaments(id) ON DELETE RESTRICT,
    category_id UUID NOT NULL REFERENCES squadra.tournament_categories(id) ON DELETE RESTRICT,
    team_id UUID NOT NULL REFERENCES squadra.teams(id) ON DELETE RESTRICT,
    inviter_user_id UUID NOT NULL REFERENCES squadra.users(id) ON DELETE RESTRICT,
    status VARCHAR(20) NOT NULL,
    pending_key BOOLEAN,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    decided_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT tournament_invitations_status_check CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT tournament_invitations_pending_key_check CHECK (
        (status = 'PENDING' AND pending_key = TRUE) OR (status <> 'PENDING' AND pending_key IS NULL)
    ),
    CONSTRAINT tournament_invitations_unique_pending UNIQUE (category_id, team_id, pending_key)
);

CREATE INDEX idx_tournament_invitations_category_created ON squadra.tournament_invitations(category_id, created_at);
CREATE INDEX idx_tournament_invitations_team_created ON squadra.tournament_invitations(team_id, created_at);
