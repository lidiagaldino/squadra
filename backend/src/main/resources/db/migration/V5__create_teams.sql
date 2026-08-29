CREATE TABLE squadra.teams (
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL REFERENCES squadra.users(id),
    name VARCHAR(100) NOT NULL CHECK (char_length(name) BETWEEN 2 AND 100),
    city VARCHAR(80),
    state VARCHAR(80),
    founded_year INTEGER CHECK (founded_year IS NULL OR founded_year >= 1850),
    description VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE squadra.team_administrators (
    team_id UUID NOT NULL REFERENCES squadra.teams(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES squadra.users(id),
    role VARCHAR(20) NOT NULL CHECK (role IN ('TEAM_OWNER', 'TEAM_ADMIN')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (team_id, user_id)
);

CREATE TABLE squadra.team_athlete_memberships (
    team_id UUID NOT NULL REFERENCES squadra.teams(id) ON DELETE CASCADE,
    athlete_user_id UUID NOT NULL REFERENCES squadra.users(id),
    status VARCHAR(20) NOT NULL CHECK (status IN ('INVITED', 'ACTIVE')),
    invited_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (team_id, athlete_user_id)
);

CREATE INDEX teams_owner_user_idx ON squadra.teams (owner_user_id);
CREATE INDEX team_administrators_user_idx ON squadra.team_administrators (user_id);
CREATE INDEX team_athlete_memberships_user_idx ON squadra.team_athlete_memberships (athlete_user_id);
