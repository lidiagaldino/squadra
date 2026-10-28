package com.squadra.platform.api.v1.teams;

import com.squadra.platform.api.filter.RequiresRoles;
import com.squadra.platform.api.v1.registrations.response.InvitationResponses;
import com.squadra.platform.application.services.interfaces.TournamentInvitationService;
import com.squadra.platform.domain.enums.UserRole;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/api/v1/teams/directory")
@Produces(MediaType.APPLICATION_JSON)
public final class TeamDirectoryResource {
    private final TournamentInvitationService invitations;

    @Inject
    public TeamDirectoryResource(TournamentInvitationService invitations) { this.invitations = invitations; }

    @GET
    @RequiresRoles(UserRole.ORGANIZER)
    public Uni<List<InvitationResponses.TeamDirectoryItem>> search(
            @DefaultValue("0") @QueryParam("page") @Min(0) @Max(100000) int page,
            @DefaultValue("20") @QueryParam("size") @Min(1) @Max(50) int size,
            @DefaultValue("") @QueryParam("query") String query) {
        return invitations.searchTeams(page, size, query).map(items -> items.stream()
                .map(team -> new InvitationResponses.TeamDirectoryItem(team.id().toString(), team.name(), team.city(), team.state())).toList());
    }
}
