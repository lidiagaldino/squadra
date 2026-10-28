package com.squadra.platform.api.v1.registrations;

import com.squadra.platform.api.filter.RequiresRoles;
import com.squadra.platform.api.v1.registrations.request.RegistrationRequests;
import com.squadra.platform.api.v1.registrations.response.RegistrationResponses;
import com.squadra.platform.application.services.interfaces.TeamRegistrationService;
import com.squadra.platform.domain.enums.UserRole;
import com.squadra.platform.domain.repositories.TeamRegistrationRepository;
import com.squadra.platform.domain.services.AccessTokenVerifier;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@Path("/api/v1/tournaments/{tournamentId}/categories/{categoryId}/registrations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public final class TeamRegistrationsResource {
    private final TeamRegistrationService registrations;

    @Inject
    public TeamRegistrationsResource(TeamRegistrationService registrations) { this.registrations = registrations; }

    @GET
    @RequiresRoles(UserRole.ORGANIZER)
    public Uni<List<RegistrationResponses.RegistrationDetails>> listCategory(@PathParam("tournamentId") UUID tournamentId,
            @PathParam("categoryId") UUID categoryId, @Context ContainerRequestContext context) {
        return registrations.listCategory(currentUser(context).userId(), tournamentId, categoryId)
                .map(items -> items.stream().map(TeamRegistrationsResource::response).toList());
    }

    @POST
    @RequiresRoles({UserRole.ATHLETE, UserRole.ORGANIZER})
    public Uni<Response> register(@PathParam("tournamentId") UUID tournamentId, @PathParam("categoryId") UUID categoryId,
            @Valid RegistrationRequests.RegisterTeam request, @Context ContainerRequestContext context) {
        UUID teamId = UUID.fromString(request.teamId());
        return registrations.register(currentUser(context).userId(), tournamentId, categoryId,
                new TeamRegistrationService.RegisterTeamCommand(teamId))
                .map(details -> Response.created(URI.create("/api/v1/teams/" + teamId + "/registrations/"
                        + details.registration().id())).entity(response(details)).build());
    }

    private static AccessTokenVerifier.VerifiedAccessToken currentUser(ContainerRequestContext request) {
        return (AccessTokenVerifier.VerifiedAccessToken) request.getProperty("access-token");
    }

    static RegistrationResponses.RegistrationDetails response(TeamRegistrationRepository.RegistrationDetails details) {
        var registration = details.registration();
        return new RegistrationResponses.RegistrationDetails(registration.id().toString(), registration.teamId().toString(),
                details.teamName(), registration.tournamentId().toString(), details.tournamentName(),
                registration.categoryId().toString(), details.categoryName(), registration.status(),
                registration.createdAt().toString(), registration.updatedAt().toString());
    }
}
