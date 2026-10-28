package com.squadra.platform.domain.exceptions;

public final class TournamentInvitationNotFoundException extends RuntimeException {
    public TournamentInvitationNotFoundException() { super("Tournament invitation was not found"); }
}
