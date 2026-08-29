package com.squadra.platform.api.v1.athletes.profile;

import com.squadra.platform.api.filter.RequiresRoles;
import com.squadra.platform.api.v1.athletes.profile.request.AthleteProfileRequests;
import com.squadra.platform.api.v1.athletes.profile.response.AthleteProfileResponses;
import com.squadra.platform.application.services.interfaces.AthleteProfileService;
import com.squadra.platform.domain.entities.AthleteProfile;
import com.squadra.platform.domain.enums.DominantFoot;
import com.squadra.platform.domain.enums.FootballPosition;
import com.squadra.platform.domain.enums.UserRole;
import com.squadra.platform.domain.exceptions.AthleteProfileNotFoundException;
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
import java.util.List;
import java.util.HashSet;

@Path("/api/v1/athletes/me/profile")
@Produces(MediaType.APPLICATION_JSON)
public final class AthleteProfileResource {
    private final AthleteProfileService profiles;

    @Inject
    public AthleteProfileResource(AthleteProfileService profiles) {
        this.profiles = profiles;
    }

    @GET
    @RequiresRoles(UserRole.ATHLETE)
    public Uni<AthleteProfileResponses.Profile> get(@Context ContainerRequestContext request) {
        return profiles.findMine(currentUser(request).userId())
                .map(profile -> profile.map(AthleteProfileResource::response).orElseThrow(AthleteProfileNotFoundException::new));
    }

    @PUT
    @RequiresRoles(UserRole.ATHLETE)
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<AthleteProfileResponses.Profile> save(@Valid AthleteProfileRequests.SaveProfile request,
            @Context ContainerRequestContext context) {
        var command = new AthleteProfileService.SaveAthleteProfileCommand(request.displayName(), request.fullName(),
                request.birthDate(), position(request.primaryPosition()), positions(request.secondaryPositions()),
                dominantFoot(request.dominantFoot()), request.jerseyNumber(), request.city(), request.state(), request.bio());
        return profiles.saveMine(currentUser(context).userId(), command).map(AthleteProfileResource::response);
    }

    private static AccessTokenVerifier.VerifiedAccessToken currentUser(ContainerRequestContext request) {
        return (AccessTokenVerifier.VerifiedAccessToken) request.getProperty("access-token");
    }

    private static AthleteProfileResponses.Profile response(AthleteProfile profile) {
        return new AthleteProfileResponses.Profile(profile.id().toString(), profile.displayName(), profile.fullName(),
                profile.birthDate() == null ? null : profile.birthDate().toString(), profile.primaryPosition().name(),
                profile.secondaryPositions().stream().map(Enum::name).sorted().toList(),
                profile.dominantFoot() == null ? null : profile.dominantFoot().name(), profile.jerseyNumber(),
                profile.city(), profile.state(), profile.bio(), profile.createdAt().toString(), profile.updatedAt().toString());
    }

    private static FootballPosition position(String value) {
        try {
            return FootballPosition.valueOf(value);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("primaryPosition is invalid");
        }
    }

    private static List<FootballPosition> positions(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        if (new HashSet<>(values).size() != values.size()) {
            throw new IllegalArgumentException("secondaryPositions cannot contain duplicates");
        }
        return values.stream().map(AthleteProfileResource::position).toList();
    }

    private static DominantFoot dominantFoot(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return DominantFoot.valueOf(value);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("dominantFoot is invalid");
        }
    }
}
