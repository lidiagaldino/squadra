package com.squadra.platform.domain.exceptions;

public class TournamentNotFoundException extends RuntimeException {
    public TournamentNotFoundException() {
        super("Tournament was not found");
    }
}
