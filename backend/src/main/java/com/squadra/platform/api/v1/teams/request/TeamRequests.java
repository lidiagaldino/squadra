package com.squadra.platform.api.v1.teams.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class TeamRequests {
    private TeamRequests() { }

    public record SaveTeam(@NotBlank @Size(min = 2, max = 100) String name, @Size(max = 80) String city,
            @Size(max = 80) String state, Integer foundedYear, @Size(max = 500) String description) { }

    public record AssignAdmin(@NotBlank @Email @Size(max = 320) String email, @NotBlank String role) { }

    public record InviteAthlete(@NotBlank @Email @Size(max = 320) String email) { }

    public record TransferOwnership(@NotNull String userId) { }
}
