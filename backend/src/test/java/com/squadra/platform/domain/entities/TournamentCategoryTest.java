package com.squadra.platform.domain.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.squadra.platform.domain.enums.CategoryPaymentModel;
import com.squadra.platform.domain.enums.CompetitionFormat;
import com.squadra.platform.domain.enums.SchedulingStrategy;
import com.squadra.platform.domain.enums.TournamentModality;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TournamentCategoryTest {
    private static final Instant NOW = Instant.parse("2026-08-29T12:00:00Z");

    @Test
    void createsCategoryWithIndependentConfigurationAndNormalizedName() {
        UUID tournamentId = UUID.randomUUID();

        TournamentCategory category = TournamentCategory.create(tournamentId, validFields(" Iniciante Feminino "), NOW);

        assertEquals(tournamentId, category.tournamentId());
        assertEquals("Iniciante Feminino", category.name());
        assertEquals("iniciante feminino", category.normalizedName());
        assertEquals(TournamentModality.FUTSAL, category.modality());
        assertEquals(7, category.minAthletes());
        assertEquals(12, category.maxAthletes());
        assertEquals(4, category.minTeams());
        assertEquals(16, category.maxTeams());
        assertEquals(CategoryPaymentModel.TEAM, category.paymentModel());
        assertEquals(12500, category.priceInCents());
        assertEquals(CompetitionFormat.GROUP_STAGE, category.competitionFormat());
        assertNull(category.swissRounds());
        assertEquals(40, category.matchDurationMinutes());
        assertEquals(10, category.intervalBetweenMatchesMinutes());
        assertEquals(60, category.minimumRestMinutes());
        assertEquals(SchedulingStrategy.BALANCED_REST, category.schedulingStrategy());
        assertEquals(NOW, category.createdAt());
        assertEquals(NOW, category.updatedAt());
    }

    @Test
    void preservesIdentityAndCreatedAtWhenUpdated() {
        TournamentCategory category = TournamentCategory.create(UUID.randomUUID(), validFields("Iniciante"), NOW);

        category.update(new TournamentCategory.SaveFields("Avancado", TournamentModality.SOCIETY, 10, 18,
                6, 24, CategoryPaymentModel.ATHLETE, 2500, CompetitionFormat.SWISS, 5,
                50, 15, 90, SchedulingStrategy.HYBRID), NOW.plusSeconds(60));

        assertEquals("Avancado", category.name());
        assertEquals(TournamentModality.SOCIETY, category.modality());
        assertEquals(CategoryPaymentModel.ATHLETE, category.paymentModel());
        assertEquals(CompetitionFormat.SWISS, category.competitionFormat());
        assertEquals(5, category.swissRounds());
        assertEquals(NOW, category.createdAt());
        assertEquals(NOW.plusSeconds(60), category.updatedAt());
    }

    @Test
    void rejectsInvalidLimitsPriceAndSchedulingSettings() {
        assertThrows(IllegalArgumentException.class, () -> TournamentCategory.create(UUID.randomUUID(), validFields("AB"), NOW));
        assertThrows(IllegalArgumentException.class, () -> TournamentCategory.create(UUID.randomUUID(), fields("Categoria", 0, 12, 4, 16, 12500, 40, 10, 60), NOW));
        assertThrows(IllegalArgumentException.class, () -> TournamentCategory.create(UUID.randomUUID(), fields("Categoria", 7, 6, 4, 16, 12500, 40, 10, 60), NOW));
        assertThrows(IllegalArgumentException.class, () -> TournamentCategory.create(UUID.randomUUID(), fields("Categoria", 7, 100, 4, 16, 12500, 40, 10, 60), NOW));
        assertThrows(IllegalArgumentException.class, () -> TournamentCategory.create(UUID.randomUUID(), fields("Categoria", 7, 12, 1, 16, 12500, 40, 10, 60), NOW));
        assertThrows(IllegalArgumentException.class, () -> TournamentCategory.create(UUID.randomUUID(), fields("Categoria", 7, 12, 4, 3, 12500, 40, 10, 60), NOW));
        assertThrows(IllegalArgumentException.class, () -> TournamentCategory.create(UUID.randomUUID(), fields("Categoria", 7, 12, 4, 513, 12500, 40, 10, 60), NOW));
        assertThrows(IllegalArgumentException.class, () -> TournamentCategory.create(UUID.randomUUID(), fields("Categoria", 7, 12, 4, 16, -1, 40, 10, 60), NOW));
        assertThrows(IllegalArgumentException.class, () -> TournamentCategory.create(UUID.randomUUID(), fields("Categoria", 7, 12, 4, 16, 12500, 9, 10, 60), NOW));
        assertThrows(IllegalArgumentException.class, () -> TournamentCategory.create(UUID.randomUUID(), fields("Categoria", 7, 12, 4, 16, 12500, 40, -1, 60), NOW));
        assertThrows(IllegalArgumentException.class, () -> TournamentCategory.create(UUID.randomUUID(), fields("Categoria", 7, 12, 4, 16, 12500, 40, 10, -1), NOW));
    }

    @Test
    void enforcesSwissRoundsOnlyForSwissFormat() {
        assertThrows(IllegalArgumentException.class, () -> TournamentCategory.create(UUID.randomUUID(),
                new TournamentCategory.SaveFields("Suico", TournamentModality.FUTSAL, 7, 12, 4, 16,
                        CategoryPaymentModel.TEAM, 0, CompetitionFormat.SWISS, null, 40, 10, 60,
                        SchedulingStrategy.BALANCED_REST), NOW));
        assertThrows(IllegalArgumentException.class, () -> TournamentCategory.create(UUID.randomUUID(),
                new TournamentCategory.SaveFields("Suico", TournamentModality.FUTSAL, 7, 12, 4, 16,
                        CategoryPaymentModel.TEAM, 0, CompetitionFormat.SWISS, 21, 40, 10, 60,
                        SchedulingStrategy.BALANCED_REST), NOW));
        assertThrows(IllegalArgumentException.class, () -> TournamentCategory.create(UUID.randomUUID(),
                new TournamentCategory.SaveFields("Grupos", TournamentModality.FUTSAL, 7, 12, 4, 16,
                        CategoryPaymentModel.TEAM, 0, CompetitionFormat.GROUP_STAGE, 3, 40, 10, 60,
                        SchedulingStrategy.BALANCED_REST), NOW));
    }

    private static TournamentCategory.SaveFields validFields(String name) {
        return fields(name, 7, 12, 4, 16, 12500, 40, 10, 60);
    }

    private static TournamentCategory.SaveFields fields(String name, int minAthletes, int maxAthletes, int minTeams,
            int maxTeams, long priceInCents, int matchDurationMinutes, int intervalBetweenMatchesMinutes,
            int minimumRestMinutes) {
        return new TournamentCategory.SaveFields(name, TournamentModality.FUTSAL, minAthletes, maxAthletes,
                minTeams, maxTeams, CategoryPaymentModel.TEAM, priceInCents, CompetitionFormat.GROUP_STAGE, null,
                matchDurationMinutes, intervalBetweenMatchesMinutes, minimumRestMinutes, SchedulingStrategy.BALANCED_REST);
    }
}
