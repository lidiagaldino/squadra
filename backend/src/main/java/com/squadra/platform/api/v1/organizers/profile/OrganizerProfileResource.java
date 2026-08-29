package com.squadra.platform.api.v1.organizers.profile;

import com.squadra.platform.api.filter.RequiresRoles;
import com.squadra.platform.api.v1.organizers.profile.request.OrganizerProfileRequests;
import com.squadra.platform.api.v1.organizers.profile.response.OrganizerProfileResponses;
import com.squadra.platform.application.services.interfaces.OrganizerProfileService;
import com.squadra.platform.domain.entities.OrganizerProfile;
import com.squadra.platform.domain.enums.UserRole;
import com.squadra.platform.domain.exceptions.OrganizerProfileNotFoundException;
import com.squadra.platform.domain.services.AccessTokenVerifier;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

@Path("/api/v1/organizers/me/profile")
@Produces(MediaType.APPLICATION_JSON)
public final class OrganizerProfileResource {
    private final OrganizerProfileService profiles;

    @Inject
    public OrganizerProfileResource(OrganizerProfileService profiles) {
        this.profiles = profiles;
    }

    @GET
    @RequiresRoles(UserRole.ORGANIZER)
    public Uni<OrganizerProfileResponses.Profile> get(@Context ContainerRequestContext request) {
        return profiles.findMine(currentUser(request).userId())
                .map(profile -> profile.map(OrganizerProfileResource::response)
                        .orElseThrow(OrganizerProfileNotFoundException::new));
    }

    @PUT
    @RequiresRoles(UserRole.ORGANIZER)
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<OrganizerProfileResponses.Profile> save(@Valid OrganizerProfileRequests.SaveProfile request,
            @Context ContainerRequestContext context) {
        var command = new OrganizerProfileService.SaveOrganizerProfileCommand(request.organizationName(),
                request.contactName(), request.contactEmail(), request.phone(), request.city(), request.state(),
                request.website(), request.description());
        return profiles.saveMine(currentUser(context).userId(), command).map(OrganizerProfileResource::response);
    }

    private static AccessTokenVerifier.VerifiedAccessToken currentUser(ContainerRequestContext request) {
        return (AccessTokenVerifier.VerifiedAccessToken) request.getProperty("access-token");
    }

    private static OrganizerProfileResponses.Profile response(OrganizerProfile profile) {
        return new OrganizerProfileResponses.Profile(profile.id().toString(), profile.organizationName(),
                profile.contactName(), profile.contactEmail().value(), profile.phone(), profile.city(),
                profile.state(), profile.website(), profile.description(), profile.createdAt().toString(),
                profile.updatedAt().toString());
    }
}
