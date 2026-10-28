package com.squadra.platform.api.v1.registrations;

import com.squadra.platform.api.filter.RequiresRoles;
import com.squadra.platform.api.v1.registrations.request.InvitationRequests;
import com.squadra.platform.api.v1.registrations.response.InvitationResponses;
import com.squadra.platform.application.services.interfaces.TournamentInvitationService;
import com.squadra.platform.domain.enums.UserRole;
import com.squadra.platform.domain.repositories.TournamentInvitationRepository;
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
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Path("/api/v1/tournaments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public final class TournamentInvitationsResource {
    private final TournamentInvitationService invitations;

    @Inject
    public TournamentInvitationsResource(TournamentInvitationService invitations) { this.invitations = invitations; }

    @GET
    @Path("/{tournamentId}/categories/{categoryId}/invitations")
    @RequiresRoles(UserRole.ORGANIZER)
    public Uni<List<InvitationResponses.InvitationDetails>> listCategory(@PathParam("tournamentId") UUID tournamentId,
            @PathParam("categoryId") UUID categoryId, @Context ContainerRequestContext context) {
        return invitations.listCategory(currentUser(context).userId(), tournamentId, categoryId)
                .map(items -> items.stream().map(TournamentInvitationsResource::response).toList());
    }

    @POST
    @Path("/{tournamentId}/categories/{categoryId}/invitations")
    @RequiresRoles(UserRole.ORGANIZER)
    public Uni<Response> invite(@PathParam("tournamentId") UUID tournamentId, @PathParam("categoryId") UUID categoryId,
            @Valid InvitationRequests.InviteTeam request, @Context ContainerRequestContext context) {
        UUID teamId = UUID.fromString(request.teamId());
        return invitations.invite(currentUser(context).userId(), tournamentId, categoryId, teamId)
                .map(details -> Response.created(URI.create("/api/v1/teams/" + teamId + "/tournament-invitations/"
                        + details.invitation().id())).entity(response(details)).build());
    }

    @POST
    @Path("/{tournamentId}/categories/{categoryId}/invitations/{invitationId}/cancel")
    @Consumes(MediaType.WILDCARD)
    @RequiresRoles(UserRole.ORGANIZER)
    public Uni<InvitationResponses.InvitationDetails> cancel(@PathParam("tournamentId") UUID tournamentId,
            @PathParam("categoryId") UUID categoryId, @PathParam("invitationId") UUID invitationId,
            @Context ContainerRequestContext context) {
        return invitations.cancel(currentUser(context).userId(), tournamentId, categoryId, invitationId)
                .map(TournamentInvitationsResource::response);
    }

    private static InvitationResponses.InvitationDetails response(TournamentInvitationRepository.InvitationDetails details) {
        var invitation = details.invitation();
        return new InvitationResponses.InvitationDetails(invitation.id().toString(), invitation.tournamentId().toString(),
                details.tournamentName(), invitation.categoryId().toString(), details.categoryName(), invitation.teamId().toString(),
                details.teamName(), invitation.status(), instant(invitation.createdAt()), instant(invitation.updatedAt()),
                instant(invitation.decidedAt()));
    }

    static AccessTokenVerifier.VerifiedAccessToken currentUser(ContainerRequestContext request) {
        return (AccessTokenVerifier.VerifiedAccessToken) request.getProperty("access-token");
    }

    private static String instant(Instant value) { return value == null ? null : value.toString(); }

}
