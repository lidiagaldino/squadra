ALTER TABLE squadra.tournament_registrations
    ADD COLUMN active_key BOOLEAN;

UPDATE squadra.tournament_registrations
SET active_key = CASE WHEN status IN ('REJECTED', 'CANCELLED') THEN NULL ELSE TRUE END;

ALTER TABLE squadra.tournament_registrations
    DROP CONSTRAINT tournament_registrations_unique_status;

ALTER TABLE squadra.tournament_registrations
    ADD CONSTRAINT tournament_registrations_active_key_check CHECK (
        (status IN ('REJECTED', 'CANCELLED') AND active_key IS NULL)
        OR (status NOT IN ('REJECTED', 'CANCELLED') AND active_key = TRUE)
    );

ALTER TABLE squadra.tournament_registrations
    ADD CONSTRAINT tournament_registrations_unique_active UNIQUE (category_id, team_id, active_key);
