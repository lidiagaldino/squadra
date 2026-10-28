package com.squadra.platform.domain.exceptions;

public final class TeamRegistrationNotFoundException extends RuntimeException {
    public TeamRegistrationNotFoundException() {
        super("Team registration was not found");
    }
}
