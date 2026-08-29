package com.squadra.platform.domain.exceptions;

public final class TeamForbiddenException extends RuntimeException {
    public TeamForbiddenException() {
        super("You cannot manage this team");
    }
}
