CREATE TABLE squadra.tournament_categories (
    id UUID PRIMARY KEY,
    tournament_id UUID NOT NULL REFERENCES squadra.tournaments(id) ON DELETE CASCADE,
    name VARCHAR(80) NOT NULL,
    normalized_name VARCHAR(80) NOT NULL,
    modality VARCHAR(30) NOT NULL,
    min_athletes INTEGER NOT NULL,
    max_athletes INTEGER NOT NULL,
    min_teams INTEGER NOT NULL,
    max_teams INTEGER NOT NULL,
    payment_model VARCHAR(20) NOT NULL,
    price_in_cents BIGINT NOT NULL,
    competition_format VARCHAR(30) NOT NULL,
    swiss_rounds INTEGER,
    match_duration_minutes INTEGER NOT NULL,
    interval_between_matches_minutes INTEGER NOT NULL,
    minimum_rest_minutes INTEGER NOT NULL,
    scheduling_strategy VARCHAR(40) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT tournament_categories_name_length_check CHECK (char_length(trim(name)) BETWEEN 3 AND 80),
    CONSTRAINT tournament_categories_unique_name UNIQUE (tournament_id, normalized_name),
    CONSTRAINT tournament_categories_modality_check CHECK (modality IN ('FIELD_SOCCER', 'FUTSAL', 'SOCIETY')),
    CONSTRAINT tournament_categories_roster_check CHECK (min_athletes BETWEEN 1 AND 99 AND max_athletes BETWEEN min_athletes AND 99),
    CONSTRAINT tournament_categories_team_check CHECK (min_teams BETWEEN 2 AND 512 AND max_teams BETWEEN min_teams AND 512),
    CONSTRAINT tournament_categories_payment_model_check CHECK (payment_model IN ('TEAM', 'ATHLETE')),
    CONSTRAINT tournament_categories_price_check CHECK (price_in_cents >= 0),
    CONSTRAINT tournament_categories_format_check CHECK (competition_format IN ('ROUND_ROBIN', 'GROUP_STAGE', 'KNOCKOUT', 'GROUPS_PLUS_CUPS', 'SWISS')),
    CONSTRAINT tournament_categories_swiss_rounds_check CHECK (
        (competition_format = 'SWISS' AND swiss_rounds BETWEEN 1 AND 20)
        OR (competition_format <> 'SWISS' AND swiss_rounds IS NULL)
    ),
    CONSTRAINT tournament_categories_schedule_check CHECK (
        match_duration_minutes BETWEEN 10 AND 240
        AND interval_between_matches_minutes BETWEEN 0 AND 240
        AND minimum_rest_minutes BETWEEN 0 AND 1440
    ),
    CONSTRAINT tournament_categories_strategy_check CHECK (scheduling_strategy IN ('FASTEST_COMPLETION', 'BALANCED_REST', 'BALANCED_COURTS', 'HYBRID', 'MANUAL'))
);

CREATE INDEX idx_tournament_categories_tournament_created ON squadra.tournament_categories(tournament_id, created_at);
