package com.squadra.platform.api.v1.registrations;

import com.squadra.platform.api.filter.RequiresRoles;
import com.squadra.platform.api.v1.registrations.response.InvitationResponses;
import com.squadra.platform.application.services.interfaces.TournamentInvitationService;
import com.squadra.platform.domain.enums.UserRole;
import com.squadra.platform.domain.repositories.TournamentInvitationRepository;
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
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Path("/api/v1/teams/{teamId}/tournament-invitations")
@Produces(MediaType.APPLICATION_JSON)
public final class TeamTournamentInvitationsResource {
    private final TournamentInvitationService invitations;

    @Inject
    public TeamTournamentInvitationsResource(TournamentInvitationService invitations) { this.invitations = invitations; }

    @GET
    @RequiresRoles({UserRole.ATHLETE, UserRole.ORGANIZER})
    public Uni<List<InvitationResponses.InvitationDetails>> list(@PathParam("teamId") UUID teamId,
            @Context ContainerRequestContext context) {
        return invitations.listTeam(TournamentInvitationsResource.currentUser(context).userId(), teamId)
                .map(items -> items.stream().map(TeamTournamentInvitationsResource::response).toList());
    }

    @POST
    @Path("/{invitationId}/accept")
    @Consumes(MediaType.WILDCARD)
    @RequiresRoles({UserRole.ATHLETE, UserRole.ORGANIZER})
    public Uni<InvitationResponses.InvitationDetails> accept(@PathParam("teamId") UUID teamId,
            @PathParam("invitationId") UUID invitationId, @Context ContainerRequestContext context) {
        return invitations.accept(TournamentInvitationsResource.currentUser(context).userId(), teamId, invitationId)
                .map(TeamTournamentInvitationsResource::response);
    }

    @POST
    @Path("/{invitationId}/reject")
    @Consumes(MediaType.WILDCARD)
    @RequiresRoles({UserRole.ATHLETE, UserRole.ORGANIZER})
    public Uni<InvitationResponses.InvitationDetails> reject(@PathParam("teamId") UUID teamId,
            @PathParam("invitationId") UUID invitationId, @Context ContainerRequestContext context) {
        return invitations.reject(TournamentInvitationsResource.currentUser(context).userId(), teamId, invitationId)
                .map(TeamTournamentInvitationsResource::response);
    }

    private static InvitationResponses.InvitationDetails response(TournamentInvitationRepository.InvitationDetails details) {
        var invitation = details.invitation();
        return new InvitationResponses.InvitationDetails(invitation.id().toString(), invitation.tournamentId().toString(),
                details.tournamentName(), invitation.categoryId().toString(), details.categoryName(), invitation.teamId().toString(),
                details.teamName(), invitation.status(), instant(invitation.createdAt()), instant(invitation.updatedAt()),
                instant(invitation.decidedAt()));
    }

    private static String instant(Instant value) { return value == null ? null : value.toString(); }
}
