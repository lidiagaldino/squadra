package com.squadra.platform.api.v1.teams;

import com.squadra.platform.api.filter.RequiresRoles;
import com.squadra.platform.api.v1.teams.request.TeamRequests;
import com.squadra.platform.api.v1.teams.response.TeamResponses;
import com.squadra.platform.application.services.interfaces.TeamService;
import com.squadra.platform.domain.enums.TeamAdminRole;
import com.squadra.platform.domain.enums.UserRole;
import com.squadra.platform.domain.repositories.TeamRepository;
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
import java.util.List;
import java.util.UUID;

@Path("/api/v1/teams")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public final class TeamsResource {
    private final TeamService teams;

    @Inject
    public TeamsResource(TeamService teams) { this.teams = teams; }

    @GET
    @RequiresRoles({UserRole.ATHLETE, UserRole.ORGANIZER})
    public Uni<List<TeamResponses.TeamSummary>> list(@Context ContainerRequestContext request) {
        return teams.listMine(currentUser(request).userId()).map(items -> items.stream().map(TeamsResource::summary).toList());
    }

    @POST
    @RequiresRoles({UserRole.ATHLETE, UserRole.ORGANIZER})
    public Uni<Response> create(@Valid TeamRequests.SaveTeam request, @Context ContainerRequestContext context) {
        return teams.create(currentUser(context).userId(), command(request))
                .map(details -> Response.created(URI.create("/api/v1/teams/" + details.team().id())).entity(details(details)).build());
    }

    @GET
    @Path("/{teamId}")
    @RequiresRoles({UserRole.ATHLETE, UserRole.ORGANIZER})
    public Uni<TeamResponses.TeamDetails> get(@PathParam("teamId") UUID teamId, @Context ContainerRequestContext request) {
        return teams.getMine(currentUser(request).userId(), teamId).map(TeamsResource::details);
    }

    @PUT
    @Path("/{teamId}")
    @RequiresRoles({UserRole.ATHLETE, UserRole.ORGANIZER})
    public Uni<TeamResponses.TeamDetails> update(@PathParam("teamId") UUID teamId, @Valid TeamRequests.SaveTeam request,
            @Context ContainerRequestContext context) {
        return teams.update(currentUser(context).userId(), teamId, command(request)).map(TeamsResource::details);
    }

    @POST
    @Path("/{teamId}/athletes/me")
    @Consumes(MediaType.WILDCARD)
    @RequiresRoles({UserRole.ATHLETE, UserRole.ORGANIZER})
    public Uni<TeamResponses.TeamDetails> joinAsAthlete(@PathParam("teamId") UUID teamId, @Context ContainerRequestContext context) {
        return teams.joinAsAthlete(currentUser(context).userId(), teamId).map(TeamsResource::details);
    }

    @PUT
    @Path("/{teamId}/admins")
    @RequiresRoles({UserRole.ATHLETE, UserRole.ORGANIZER})
    public Uni<TeamResponses.TeamDetails> assignAdmin(@PathParam("teamId") UUID teamId,
            @Valid TeamRequests.AssignAdmin request, @Context ContainerRequestContext context) {
        return teams.assignAdmin(currentUser(context).userId(), teamId,
                new TeamService.AssignAdminCommand(request.email(), TeamAdminRole.permanentValueOf(request.role()))).map(TeamsResource::details);
    }

    @DELETE
    @Path("/{teamId}/admins/{userId}")
    @RequiresRoles({UserRole.ATHLETE, UserRole.ORGANIZER})
    public Uni<TeamResponses.TeamDetails> removeAdmin(@PathParam("teamId") UUID teamId, @PathParam("userId") UUID userId,
            @Context ContainerRequestContext context) {
        return teams.removeAdmin(currentUser(context).userId(), teamId, userId).map(TeamsResource::details);
    }

    @PUT
    @Path("/{teamId}/owner")
    @RequiresRoles({UserRole.ATHLETE, UserRole.ORGANIZER})
    public Uni<TeamResponses.TeamDetails> transferOwnership(@PathParam("teamId") UUID teamId,
            @Valid TeamRequests.TransferOwnership request, @Context ContainerRequestContext context) {
        return teams.transferOwnership(currentUser(context).userId(), teamId, UUID.fromString(request.userId())).map(TeamsResource::details);
    }

    @POST
    @Path("/{teamId}/athlete-invitations")
    @RequiresRoles({UserRole.ATHLETE, UserRole.ORGANIZER})
    public Uni<TeamResponses.TeamDetails> inviteAthlete(@PathParam("teamId") UUID teamId,
            @Valid TeamRequests.InviteAthlete request, @Context ContainerRequestContext context) {
        return teams.inviteAthlete(currentUser(context).userId(), teamId, new TeamService.InviteAthleteCommand(request.email())).map(TeamsResource::details);
    }

    @POST
    @Path("/{teamId}/athlete-invitations/accept")
    @Consumes(MediaType.WILDCARD)
    @RequiresRoles(UserRole.ATHLETE)
    public Uni<TeamResponses.TeamDetails> acceptInvitation(@PathParam("teamId") UUID teamId, @Context ContainerRequestContext context) {
        return teams.acceptInvitation(currentUser(context).userId(), teamId).map(TeamsResource::details);
    }

    @POST
    @Path("/{teamId}/athlete-invitations/reject")
    @Consumes(MediaType.WILDCARD)
    @RequiresRoles(UserRole.ATHLETE)
    public Uni<Response> rejectInvitation(@PathParam("teamId") UUID teamId, @Context ContainerRequestContext context) {
        return teams.rejectInvitation(currentUser(context).userId(), teamId).replaceWith(Response.noContent().build());
    }

    @DELETE
    @Path("/{teamId}/athlete-invitations/{userId}")
    @RequiresRoles({UserRole.ATHLETE, UserRole.ORGANIZER})
    public Uni<TeamResponses.TeamDetails> cancelInvitation(@PathParam("teamId") UUID teamId, @PathParam("userId") UUID userId,
            @Context ContainerRequestContext context) {
        return teams.cancelInvitation(currentUser(context).userId(), teamId, userId).map(TeamsResource::details);
    }

    @DELETE
    @Path("/{teamId}/athletes/{userId}")
    @RequiresRoles({UserRole.ATHLETE, UserRole.ORGANIZER})
    public Uni<TeamResponses.TeamDetails> removeAthlete(@PathParam("teamId") UUID teamId, @PathParam("userId") UUID userId,
            @Context ContainerRequestContext context) {
        return teams.removeActiveAthlete(currentUser(context).userId(), teamId, userId).map(TeamsResource::details);
    }

    private static TeamService.SaveTeamCommand command(TeamRequests.SaveTeam request) {
        return new TeamService.SaveTeamCommand(request.name(), request.city(), request.state(), request.foundedYear(), request.description());
    }

    private static AccessTokenVerifier.VerifiedAccessToken currentUser(ContainerRequestContext request) {
        return (AccessTokenVerifier.VerifiedAccessToken) request.getProperty("access-token");
    }

    private static TeamResponses.TeamSummary summary(TeamRepository.TeamSummary summary) {
        var team = summary.team();
        return new TeamResponses.TeamSummary(team.id().toString(), team.name(), team.city(), team.state(), team.foundedYear(),
                team.description(), summary.administrationRole(), summary.athleteMembershipStatus(), team.createdAt().toString(), team.updatedAt().toString());
    }

    private static TeamResponses.TeamDetails details(TeamRepository.TeamDetails details) {
        var team = details.team();
        return new TeamResponses.TeamDetails(team.id().toString(), team.name(), team.city(), team.state(), team.foundedYear(),
                team.description(), details.administrationRole(), details.athleteMembershipStatus(),
                details.administrators().stream().map(admin -> new TeamResponses.Administrator(admin.userId().toString(), admin.email(), admin.role())).toList(),
                details.athletes().stream().map(athlete -> new TeamResponses.Athlete(athlete.userId().toString(), athlete.email(), athlete.status())).toList(),
                team.createdAt().toString(), team.updatedAt().toString());
    }
}
