package com.squadra.platform.api.v1.tournaments;

import com.squadra.platform.api.filter.RequiresRoles;
import com.squadra.platform.api.v1.tournaments.response.TournamentBrowseResponses;
import com.squadra.platform.api.v1.tournaments.response.TournamentCategoryResponses;
import com.squadra.platform.api.v1.tournaments.response.TournamentResponses;
import com.squadra.platform.application.services.interfaces.TournamentDiscoveryService;
import com.squadra.platform.domain.entities.TournamentCategory;
import com.squadra.platform.domain.enums.UserRole;
import com.squadra.platform.domain.repositories.TournamentCategoryRepository;
import com.squadra.platform.domain.repositories.TournamentRepository;
import com.squadra.platform.domain.services.AccessTokenVerifier;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Path("/api/v1/tournaments")
@Produces(MediaType.APPLICATION_JSON)
public final class TournamentDiscoveryResource {
    private final TournamentDiscoveryService discovery;

    @Inject
    public TournamentDiscoveryResource(TournamentDiscoveryService discovery) { this.discovery = discovery; }

    @GET
    @Path("/available")
    @RequiresRoles({UserRole.ATHLETE, UserRole.ORGANIZER})
    public Uni<List<TournamentResponses.TournamentSummary>> available(
            @DefaultValue("0") @QueryParam("page") @Min(0) @Max(100000) int page,
            @DefaultValue("20") @QueryParam("size") @Min(1) @Max(50) int size) {
        return discovery.available(page, size).map(items -> items.stream().map(TournamentDiscoveryResource::summary).toList());
    }

    @GET
    @Path("/{tournamentId}/overview")
    @RequiresRoles({UserRole.ATHLETE, UserRole.ORGANIZER})
    public Uni<TournamentBrowseResponses.TournamentBrowse> overview(@PathParam("tournamentId") UUID tournamentId,
            @Context ContainerRequestContext request) {
        return discovery.overview(currentUser(request).userId(), tournamentId)
                .map(details -> new TournamentBrowseResponses.TournamentBrowse(tournament(details.tournament()),
                        details.categories().stream().map(TournamentDiscoveryResource::category).toList()));
    }

    private static TournamentResponses.TournamentSummary summary(TournamentRepository.TournamentSummary summary) {
        var tournament = summary.tournament();
        return new TournamentResponses.TournamentSummary(tournament.id().toString(), tournament.ownerOrganizerUserId().toString(),
                tournament.status(), tournament.name(), tournament.description(), tournament.city(), tournament.state(),
                tournament.venueName(), date(tournament.startDate()), date(tournament.endDate()), date(tournament.registrationDeadline()),
                tournament.rules(), summary.categoryCount(), instant(tournament.publishedAt()), instant(tournament.closedAt()),
                instant(tournament.cancelledAt()), instant(tournament.createdAt()), instant(tournament.updatedAt()), tournament.visibility());
    }

    private static TournamentResponses.TournamentDetails tournament(TournamentRepository.TournamentDetails details) {
        var tournament = details.tournament();
        return new TournamentResponses.TournamentDetails(tournament.id().toString(), tournament.ownerOrganizerUserId().toString(),
                tournament.status(), tournament.name(), tournament.description(), tournament.city(), tournament.state(),
                tournament.venueName(), date(tournament.startDate()), date(tournament.endDate()), date(tournament.registrationDeadline()),
                tournament.rules(), details.categoryCount(), instant(tournament.publishedAt()), instant(tournament.closedAt()),
                instant(tournament.cancelledAt()), instant(tournament.createdAt()), instant(tournament.updatedAt()), tournament.visibility());
    }

    private static TournamentCategoryResponses.CategoryDetails category(TournamentCategory category) {
        return new TournamentCategoryResponses.CategoryDetails(category.id().toString(), category.tournamentId().toString(),
                category.name(), category.modality(), category.minAthletes(), category.maxAthletes(), category.minTeams(),
                category.maxTeams(), category.paymentModel(), category.priceInCents(), category.competitionFormat(),
                category.swissRounds(), category.matchDurationMinutes(), category.intervalBetweenMatchesMinutes(),
                category.minimumRestMinutes(), category.schedulingStrategy(), instant(category.createdAt()), instant(category.updatedAt()));
    }

    private static AccessTokenVerifier.VerifiedAccessToken currentUser(ContainerRequestContext request) {
        return (AccessTokenVerifier.VerifiedAccessToken) request.getProperty("access-token");
    }

    private static String date(LocalDate value) { return value == null ? null : value.toString(); }
    private static String instant(Instant value) { return value == null ? null : value.toString(); }
}
