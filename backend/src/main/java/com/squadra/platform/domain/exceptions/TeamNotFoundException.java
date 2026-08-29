package com.squadra.platform.domain.exceptions;

public final class TeamNotFoundException extends RuntimeException {
    public TeamNotFoundException() {
        super("Team not found");
    }
}
