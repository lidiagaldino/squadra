package com.squadra.platform.domain.exceptions;

public final class TournamentCategoryConflictException extends RuntimeException {
    public TournamentCategoryConflictException(String message) {
        super(message);
    }
}
