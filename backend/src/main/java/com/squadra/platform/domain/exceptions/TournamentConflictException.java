package com.squadra.platform.domain.exceptions;

public class TournamentConflictException extends RuntimeException {
    public TournamentConflictException(String message) {
        super(message);
    }
}
