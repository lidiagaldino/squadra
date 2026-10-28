package com.squadra.platform.domain.exceptions;

public final class TournamentCategoryNotFoundException extends RuntimeException {
    public TournamentCategoryNotFoundException() {
        super("Tournament category not found");
    }
}
