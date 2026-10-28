ALTER TABLE squadra.tournaments
    ADD COLUMN visibility VARCHAR(10) NOT NULL DEFAULT 'PUBLIC';

ALTER TABLE squadra.tournaments
    ADD CONSTRAINT tournaments_visibility_check CHECK (visibility IN ('PUBLIC', 'PRIVATE'));
