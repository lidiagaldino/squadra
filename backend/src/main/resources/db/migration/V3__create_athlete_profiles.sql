CREATE TABLE squadra.athlete_profiles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES squadra.users(id) ON DELETE CASCADE,
    display_name VARCHAR(60) NOT NULL CHECK (char_length(display_name) BETWEEN 2 AND 60),
    full_name VARCHAR(120),
    birth_date DATE CHECK (birth_date IS NULL OR birth_date <= CURRENT_DATE),
    primary_position VARCHAR(20) NOT NULL CHECK (primary_position IN ('GOALKEEPER', 'DEFENDER', 'MIDFIELDER', 'FORWARD')),
    secondary_positions VARCHAR(80) NOT NULL DEFAULT '',
    dominant_foot VARCHAR(10) CHECK (dominant_foot IS NULL OR dominant_foot IN ('LEFT', 'RIGHT', 'BOTH')),
    jersey_number INTEGER CHECK (jersey_number IS NULL OR jersey_number BETWEEN 1 AND 99),
    city VARCHAR(80),
    state VARCHAR(80),
    bio VARCHAR(280),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX athlete_profiles_user_idx
    ON squadra.athlete_profiles (user_id);
