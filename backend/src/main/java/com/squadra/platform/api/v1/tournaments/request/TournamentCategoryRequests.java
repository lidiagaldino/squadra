package com.squadra.platform.api.v1.tournaments.request;

import com.squadra.platform.domain.enums.CategoryPaymentModel;
import com.squadra.platform.domain.enums.CompetitionFormat;
import com.squadra.platform.domain.enums.SchedulingStrategy;
import com.squadra.platform.domain.enums.TournamentModality;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class TournamentCategoryRequests {
    private TournamentCategoryRequests() { }

    public record SaveCategory(
            @NotBlank @Size(min = 3, max = 80) String name,
            @NotNull TournamentModality modality,
            @Min(1) @Max(99) int minAthletes,
            @Min(1) @Max(99) int maxAthletes,
            @Min(2) @Max(512) int minTeams,
            @Min(2) @Max(512) int maxTeams,
            @NotNull CategoryPaymentModel paymentModel,
            @Min(0) long priceInCents,
            @NotNull CompetitionFormat competitionFormat,
            @Min(1) @Max(20) Integer swissRounds,
            @Min(10) @Max(240) int matchDurationMinutes,
            @Min(0) @Max(240) int intervalBetweenMatchesMinutes,
            @Min(0) @Max(1440) int minimumRestMinutes,
            @NotNull SchedulingStrategy schedulingStrategy) { }
}
