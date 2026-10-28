package com.squadra.platform.application.services.interfaces;

import com.squadra.platform.domain.entities.TournamentCategory;
import com.squadra.platform.domain.enums.CategoryPaymentModel;
import com.squadra.platform.domain.enums.CompetitionFormat;
import com.squadra.platform.domain.enums.SchedulingStrategy;
import com.squadra.platform.domain.enums.TournamentModality;
import io.smallrye.mutiny.Uni;
import java.util.List;
import java.util.UUID;

public interface TournamentCategoryService {
    Uni<TournamentCategory> create(UUID userId, UUID tournamentId, SaveCategoryCommand command);
    Uni<List<TournamentCategory>> list(UUID userId, UUID tournamentId);
    Uni<TournamentCategory> get(UUID userId, UUID tournamentId, UUID categoryId);
    Uni<TournamentCategory> update(UUID userId, UUID tournamentId, UUID categoryId, SaveCategoryCommand command);
    Uni<Void> delete(UUID userId, UUID tournamentId, UUID categoryId);

    record SaveCategoryCommand(String name, TournamentModality modality, int minAthletes, int maxAthletes,
            int minTeams, int maxTeams, CategoryPaymentModel paymentModel, long priceInCents,
            CompetitionFormat competitionFormat, Integer swissRounds, int matchDurationMinutes,
            int intervalBetweenMatchesMinutes, int minimumRestMinutes, SchedulingStrategy schedulingStrategy) { }
}
