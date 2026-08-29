package com.squadra.platform.domain.exceptions;

public final class TeamTargetUserNotFoundException extends RuntimeException {
    public TeamTargetUserNotFoundException() {
        super("Target user not found");
    }
}
