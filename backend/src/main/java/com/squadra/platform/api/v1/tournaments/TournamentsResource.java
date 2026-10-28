package com.squadra.platform.api.v1.tournaments;

import com.squadra.platform.api.filter.RequiresRoles;
import com.squadra.platform.api.v1.tournaments.request.TournamentRequests;
import com.squadra.platform.api.v1.tournaments.response.TournamentResponses;
import com.squadra.platform.application.services.interfaces.TournamentService;
import com.squadra.platform.domain.enums.UserRole;
import com.squadra.platform.domain.repositories.TournamentRepository;
import com.squadra.platform.domain.services.AccessTokenVerifier;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Path("/api/v1/tournaments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public final class TournamentsResource {
    private final TournamentService tournaments;

    @Inject
    public TournamentsResource(TournamentService tournaments) { this.tournaments = tournaments; }

    @GET
    @RequiresRoles(UserRole.ORGANIZER)
    public Uni<List<TournamentResponses.TournamentSummary>> list(@Context ContainerRequestContext request) {
        return tournaments.listMine(currentUser(request).userId()).map(items -> items.stream().map(TournamentsResource::summary).toList());
    }

    @POST
    @RequiresRoles(UserRole.ORGANIZER)
    public Uni<Response> create(@Valid TournamentRequests.SaveTournament request, @Context ContainerRequestContext context) {
        return tournaments.create(currentUser(context).userId(), command(request))
                .map(details -> Response.created(URI.create("/api/v1/tournaments/" + details.tournament().id()))
                        .entity(details(details)).build());
    }

    @GET
    @Path("/{tournamentId}")
    @RequiresRoles(UserRole.ORGANIZER)
    public Uni<TournamentResponses.TournamentDetails> get(@PathParam("tournamentId") UUID tournamentId,
            @Context ContainerRequestContext request) {
        return tournaments.getMine(currentUser(request).userId(), tournamentId).map(TournamentsResource::details);
    }

    @PUT
    @Path("/{tournamentId}")
    @RequiresRoles(UserRole.ORGANIZER)
    public Uni<TournamentResponses.TournamentDetails> update(@PathParam("tournamentId") UUID tournamentId,
            @Valid TournamentRequests.SaveTournament request, @Context ContainerRequestContext context) {
        return tournaments.update(currentUser(context).userId(), tournamentId, command(request)).map(TournamentsResource::details);
    }

    @POST
    @Path("/{tournamentId}/publish")
    @Consumes(MediaType.WILDCARD)
    @RequiresRoles(UserRole.ORGANIZER)
    public Uni<TournamentResponses.TournamentDetails> publish(@PathParam("tournamentId") UUID tournamentId,
            @Context ContainerRequestContext context) {
        return tournaments.publish(currentUser(context).userId(), tournamentId).map(TournamentsResource::details);
    }

    @POST
    @Path("/{tournamentId}/close")
    @Consumes(MediaType.WILDCARD)
    @RequiresRoles(UserRole.ORGANIZER)
    public Uni<TournamentResponses.TournamentDetails> close(@PathParam("tournamentId") UUID tournamentId,
            @Context ContainerRequestContext context) {
        return tournaments.close(currentUser(context).userId(), tournamentId).map(TournamentsResource::details);
    }

    @POST
    @Path("/{tournamentId}/reopen")
    @Consumes(MediaType.WILDCARD)
    @RequiresRoles(UserRole.ORGANIZER)
    public Uni<TournamentResponses.TournamentDetails> reopen(@PathParam("tournamentId") UUID tournamentId,
            @Context ContainerRequestContext context) {
        return tournaments.reopen(currentUser(context).userId(), tournamentId).map(TournamentsResource::details);
    }

    @POST
    @Path("/{tournamentId}/cancel")
    @Consumes(MediaType.WILDCARD)
    @RequiresRoles(UserRole.ORGANIZER)
    public Uni<TournamentResponses.TournamentDetails> cancel(@PathParam("tournamentId") UUID tournamentId,
            @Context ContainerRequestContext context) {
        return tournaments.cancel(currentUser(context).userId(), tournamentId).map(TournamentsResource::details);
    }

    private static TournamentService.SaveTournamentCommand command(TournamentRequests.SaveTournament request) {
        return new TournamentService.SaveTournamentCommand(request.name(), request.description(), request.city(),
                request.state(), request.venueName(), request.startDate(), request.endDate(),
                request.registrationDeadline(), request.rules(), request.visibility());
    }

    private static AccessTokenVerifier.VerifiedAccessToken currentUser(ContainerRequestContext request) {
        return (AccessTokenVerifier.VerifiedAccessToken) request.getProperty("access-token");
    }

    private static TournamentResponses.TournamentSummary summary(TournamentRepository.TournamentSummary summary) {
        var tournament = summary.tournament();
        return new TournamentResponses.TournamentSummary(tournament.id().toString(), tournament.ownerOrganizerUserId().toString(),
                tournament.status(), tournament.name(), tournament.description(), tournament.city(), tournament.state(),
                tournament.venueName(), date(tournament.startDate()), date(tournament.endDate()),
                date(tournament.registrationDeadline()), tournament.rules(), summary.categoryCount(),
                instant(tournament.publishedAt()), instant(tournament.closedAt()), instant(tournament.cancelledAt()),
                instant(tournament.createdAt()), instant(tournament.updatedAt()), tournament.visibility());
    }

    private static TournamentResponses.TournamentDetails details(TournamentRepository.TournamentDetails details) {
        var tournament = details.tournament();
        return new TournamentResponses.TournamentDetails(tournament.id().toString(), tournament.ownerOrganizerUserId().toString(),
                tournament.status(), tournament.name(), tournament.description(), tournament.city(), tournament.state(),
                tournament.venueName(), date(tournament.startDate()), date(tournament.endDate()),
                date(tournament.registrationDeadline()), tournament.rules(), details.categoryCount(),
                instant(tournament.publishedAt()), instant(tournament.closedAt()), instant(tournament.cancelledAt()),
                instant(tournament.createdAt()), instant(tournament.updatedAt()), tournament.visibility());
    }

    private static String date(LocalDate date) {
        return date == null ? null : date.toString();
    }

    private static String instant(Instant instant) {
        return instant == null ? null : instant.toString();
    }
}
