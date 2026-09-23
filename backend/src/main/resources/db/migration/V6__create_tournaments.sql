CREATE TABLE squadra.tournaments (
    id UUID PRIMARY KEY,
    owner_organizer_user_id UUID NOT NULL REFERENCES squadra.users(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(1000),
    city VARCHAR(80) NOT NULL,
    state VARCHAR(80) NOT NULL,
    venue_name VARCHAR(120) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    registration_deadline DATE NOT NULL,
    rules VARCHAR(5000),
    published_at TIMESTAMP WITH TIME ZONE,
    closed_at TIMESTAMP WITH TIME ZONE,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT tournaments_status_check CHECK (status IN ('DRAFT', 'OPEN', 'CLOSED', 'CANCELLED')),
    CONSTRAINT tournaments_name_length_check CHECK (char_length(trim(name)) BETWEEN 3 AND 120),
    CONSTRAINT tournaments_city_length_check CHECK (char_length(trim(city)) BETWEEN 2 AND 80),
    CONSTRAINT tournaments_state_length_check CHECK (char_length(trim(state)) BETWEEN 2 AND 80),
    CONSTRAINT tournaments_venue_length_check CHECK (char_length(trim(venue_name)) BETWEEN 2 AND 120),
    CONSTRAINT tournaments_description_length_check CHECK (description IS NULL OR char_length(description) <= 1000),
    CONSTRAINT tournaments_rules_length_check CHECK (rules IS NULL OR char_length(rules) <= 5000),
    CONSTRAINT tournaments_date_order_check CHECK (registration_deadline <= start_date AND start_date <= end_date)
);

CREATE INDEX idx_tournaments_owner_updated ON squadra.tournaments(owner_organizer_user_id, updated_at DESC);
CREATE INDEX idx_tournaments_owner_status ON squadra.tournaments(owner_organizer_user_id, status);
CREATE INDEX idx_tournaments_start_date ON squadra.tournaments(start_date);
