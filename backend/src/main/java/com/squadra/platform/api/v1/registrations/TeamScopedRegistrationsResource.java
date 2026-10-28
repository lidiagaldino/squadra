package com.squadra.platform.api.v1.registrations;

import com.squadra.platform.api.filter.RequiresRoles;
import com.squadra.platform.api.v1.registrations.response.RegistrationResponses;
import com.squadra.platform.application.services.interfaces.TeamRegistrationService;
import com.squadra.platform.domain.enums.UserRole;
import com.squadra.platform.domain.services.AccessTokenVerifier;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.UUID;

@Path("/api/v1/teams/{teamId}/registrations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public final class TeamScopedRegistrationsResource {
    private final TeamRegistrationService registrations;

    @Inject
    public TeamScopedRegistrationsResource(TeamRegistrationService registrations) { this.registrations = registrations; }

    @GET
    @RequiresRoles({UserRole.ATHLETE, UserRole.ORGANIZER})
    public Uni<List<RegistrationResponses.RegistrationDetails>> listTeam(@PathParam("teamId") UUID teamId,
            @Context ContainerRequestContext context) {
        return registrations.listTeam(currentUser(context).userId(), teamId)
                .map(items -> items.stream().map(TeamRegistrationsResource::response).toList());
    }

    @POST
    @Path("/{registrationId}/cancel")
    @Consumes(MediaType.WILDCARD)
    @RequiresRoles({UserRole.ATHLETE, UserRole.ORGANIZER})
    public Uni<RegistrationResponses.RegistrationDetails> cancel(@PathParam("teamId") UUID teamId,
            @PathParam("registrationId") UUID registrationId, @Context ContainerRequestContext context) {
        return registrations.cancelTeamRegistration(currentUser(context).userId(), teamId, registrationId)
                .map(TeamRegistrationsResource::response);
    }

    private static AccessTokenVerifier.VerifiedAccessToken currentUser(ContainerRequestContext request) {
        return (AccessTokenVerifier.VerifiedAccessToken) request.getProperty("access-token");
    }
}
