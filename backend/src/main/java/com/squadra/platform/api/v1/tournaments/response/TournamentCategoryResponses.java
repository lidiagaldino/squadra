package com.squadra.platform.api.v1.tournaments.response;

import com.squadra.platform.domain.enums.CategoryPaymentModel;
import com.squadra.platform.domain.enums.CompetitionFormat;
import com.squadra.platform.domain.enums.SchedulingStrategy;
import com.squadra.platform.domain.enums.TournamentModality;

public final class TournamentCategoryResponses {
    private TournamentCategoryResponses() { }

    public record CategoryDetails(String id, String tournamentId, String name, TournamentModality modality,
            int minAthletes, int maxAthletes, int minTeams, int maxTeams, CategoryPaymentModel paymentModel,
            long priceInCents, CompetitionFormat competitionFormat, Integer swissRounds, int matchDurationMinutes,
            int intervalBetweenMatchesMinutes, int minimumRestMinutes, SchedulingStrategy schedulingStrategy,
            String createdAt, String updatedAt) { }
}
