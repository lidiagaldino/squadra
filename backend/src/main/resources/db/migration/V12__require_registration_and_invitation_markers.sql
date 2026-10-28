ALTER TABLE squadra.tournament_invitations
    DROP CONSTRAINT tournament_invitations_pending_key_check;
ALTER TABLE squadra.tournament_invitations
    ADD CONSTRAINT tournament_invitations_pending_key_check CHECK (
        (status = 'PENDING' AND pending_key IS NOT NULL AND pending_key = TRUE)
        OR (status <> 'PENDING' AND pending_key IS NULL)
    );

ALTER TABLE squadra.tournament_registrations
    DROP CONSTRAINT tournament_registrations_active_key_check;
ALTER TABLE squadra.tournament_registrations
    ADD CONSTRAINT tournament_registrations_active_key_check CHECK (
        (status IN ('REJECTED', 'CANCELLED') AND active_key IS NULL)
        OR (status NOT IN ('REJECTED', 'CANCELLED') AND active_key IS NOT NULL AND active_key = TRUE)
    );
