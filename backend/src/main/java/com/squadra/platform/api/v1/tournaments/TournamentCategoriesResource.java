package com.squadra.platform.api.v1.tournaments;

import com.squadra.platform.api.filter.RequiresRoles;
import com.squadra.platform.api.v1.tournaments.request.TournamentCategoryRequests;
import com.squadra.platform.api.v1.tournaments.response.TournamentCategoryResponses;
import com.squadra.platform.application.services.interfaces.TournamentCategoryService;
import com.squadra.platform.domain.entities.TournamentCategory;
import com.squadra.platform.domain.enums.UserRole;
import com.squadra.platform.domain.services.AccessTokenVerifier;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
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
import java.util.List;
import java.util.UUID;

@Path("/api/v1/tournaments/{tournamentId}/categories")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public final class TournamentCategoriesResource {
    private final TournamentCategoryService categories;

    @Inject
    public TournamentCategoriesResource(TournamentCategoryService categories) { this.categories = categories; }

    @GET
    @RequiresRoles(UserRole.ORGANIZER)
    public Uni<List<TournamentCategoryResponses.CategoryDetails>> list(@PathParam("tournamentId") UUID tournamentId,
            @Context ContainerRequestContext request) {
        return categories.list(currentUser(request).userId(), tournamentId)
                .map(items -> items.stream().map(TournamentCategoriesResource::response).toList());
    }

    @POST
    @RequiresRoles(UserRole.ORGANIZER)
    public Uni<Response> create(@PathParam("tournamentId") UUID tournamentId,
            @Valid TournamentCategoryRequests.SaveCategory request, @Context ContainerRequestContext context) {
        return categories.create(currentUser(context).userId(), tournamentId, command(request))
                .map(category -> Response.created(URI.create("/api/v1/tournaments/" + tournamentId + "/categories/" + category.id()))
                        .entity(response(category)).build());
    }

    @GET
    @Path("/{categoryId}")
    @RequiresRoles(UserRole.ORGANIZER)
    public Uni<TournamentCategoryResponses.CategoryDetails> get(@PathParam("tournamentId") UUID tournamentId,
            @PathParam("categoryId") UUID categoryId, @Context ContainerRequestContext request) {
        return categories.get(currentUser(request).userId(), tournamentId, categoryId).map(TournamentCategoriesResource::response);
    }

    @PUT
    @Path("/{categoryId}")
    @RequiresRoles(UserRole.ORGANIZER)
    public Uni<TournamentCategoryResponses.CategoryDetails> update(@PathParam("tournamentId") UUID tournamentId,
            @PathParam("categoryId") UUID categoryId, @Valid TournamentCategoryRequests.SaveCategory request,
            @Context ContainerRequestContext context) {
        return categories.update(currentUser(context).userId(), tournamentId, categoryId, command(request))
                .map(TournamentCategoriesResource::response);
    }

    @DELETE
    @Path("/{categoryId}")
    @Consumes(MediaType.WILDCARD)
    @RequiresRoles(UserRole.ORGANIZER)
    public Uni<Response> delete(@PathParam("tournamentId") UUID tournamentId, @PathParam("categoryId") UUID categoryId,
            @Context ContainerRequestContext context) {
        return categories.delete(currentUser(context).userId(), tournamentId, categoryId)
                .replaceWith(Response.noContent().build());
    }

    private static TournamentCategoryService.SaveCategoryCommand command(TournamentCategoryRequests.SaveCategory request) {
        return new TournamentCategoryService.SaveCategoryCommand(request.name(), request.modality(), request.minAthletes(),
                request.maxAthletes(), request.minTeams(), request.maxTeams(), request.paymentModel(),
                request.priceInCents(), request.competitionFormat(), request.swissRounds(),
                request.matchDurationMinutes(), request.intervalBetweenMatchesMinutes(), request.minimumRestMinutes(),
                request.schedulingStrategy());
    }

    private static AccessTokenVerifier.VerifiedAccessToken currentUser(ContainerRequestContext request) {
        return (AccessTokenVerifier.VerifiedAccessToken) request.getProperty("access-token");
    }

    private static TournamentCategoryResponses.CategoryDetails response(TournamentCategory category) {
        return new TournamentCategoryResponses.CategoryDetails(category.id().toString(), category.tournamentId().toString(),
                category.name(), category.modality(), category.minAthletes(), category.maxAthletes(),
                category.minTeams(), category.maxTeams(), category.paymentModel(), category.priceInCents(),
                category.competitionFormat(), category.swissRounds(), category.matchDurationMinutes(),
                category.intervalBetweenMatchesMinutes(), category.minimumRestMinutes(), category.schedulingStrategy(),
                instant(category.createdAt()), instant(category.updatedAt()));
    }

    private static String instant(Instant instant) {
        return instant == null ? null : instant.toString();
    }
}
