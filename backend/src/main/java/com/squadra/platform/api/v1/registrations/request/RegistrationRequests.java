package com.squadra.platform.api.v1.registrations.request;

import jakarta.validation.constraints.NotBlank;

public final class RegistrationRequests {
    private RegistrationRequests() { }

    public record RegisterTeam(@NotBlank String teamId) { }
}
