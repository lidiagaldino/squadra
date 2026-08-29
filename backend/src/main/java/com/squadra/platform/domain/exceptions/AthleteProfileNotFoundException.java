package com.squadra.platform.domain.exceptions;

public final class AthleteProfileNotFoundException extends RuntimeException {
    public AthleteProfileNotFoundException() {
        super("Athlete profile was not found");
    }
}
