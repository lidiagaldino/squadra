package com.squadra.platform.domain.exceptions;

public final class OrganizerProfileNotFoundException extends RuntimeException {
    public OrganizerProfileNotFoundException() {
        super("Organizer profile was not found");
    }
}
