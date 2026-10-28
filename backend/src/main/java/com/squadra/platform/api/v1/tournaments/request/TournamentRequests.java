package com.squadra.platform.api.v1.tournaments.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import com.squadra.platform.domain.enums.TournamentVisibility;

public final class TournamentRequests {
    private TournamentRequests() { }

    public record SaveTournament(
            @NotBlank @Size(min = 3, max = 120) String name,
            @Size(max = 1000) String description,
            @NotBlank @Size(min = 2, max = 80) String city,
            @NotBlank @Size(min = 2, max = 80) String state,
            @NotBlank @Size(min = 2, max = 120) String venueName,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            @NotNull LocalDate registrationDeadline,
            @Size(max = 5000) String rules,
            TournamentVisibility visibility) { }
}
