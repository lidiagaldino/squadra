package com.squadra.platform.domain.exceptions;

public final class TeamRegistrationConflictException extends RuntimeException {
    public TeamRegistrationConflictException(String message) {
        super(message);
    }
}
