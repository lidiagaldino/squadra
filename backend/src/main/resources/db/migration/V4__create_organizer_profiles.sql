CREATE TABLE squadra.organizer_profiles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES squadra.users(id) ON DELETE CASCADE,
    organization_name VARCHAR(100) NOT NULL CHECK (char_length(organization_name) BETWEEN 2 AND 100),
    contact_name VARCHAR(100) NOT NULL CHECK (char_length(contact_name) BETWEEN 2 AND 100),
    contact_email VARCHAR(320) NOT NULL CHECK (char_length(contact_email) BETWEEN 1 AND 320),
    phone VARCHAR(30),
    city VARCHAR(80) NOT NULL CHECK (char_length(city) BETWEEN 2 AND 80),
    state VARCHAR(80) NOT NULL CHECK (char_length(state) BETWEEN 2 AND 80),
    website VARCHAR(200) CHECK (website IS NULL OR website LIKE 'http://%' OR website LIKE 'https://%'),
    description VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX organizer_profiles_user_idx
    ON squadra.organizer_profiles (user_id);
