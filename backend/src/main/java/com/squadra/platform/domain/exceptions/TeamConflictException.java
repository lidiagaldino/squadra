package com.squadra.platform.domain.exceptions;

public final class TeamConflictException extends RuntimeException {
    public TeamConflictException() {
        super("Team operation conflicts with current state");
    }
}
