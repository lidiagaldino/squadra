package com.squadra.platform.api.v1.registrations.request;

import jakarta.validation.constraints.NotBlank;

public final class InvitationRequests {
    private InvitationRequests() { }
    public record InviteTeam(@NotBlank String teamId) { }
}
