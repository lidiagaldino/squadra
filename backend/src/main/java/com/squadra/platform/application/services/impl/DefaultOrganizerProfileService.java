package com.squadra.platform.application.services.impl;

import com.squadra.platform.application.services.interfaces.OrganizerProfileService;
import com.squadra.platform.domain.entities.OrganizerProfile;
import com.squadra.platform.domain.repositories.OrganizerProfileRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public final class DefaultOrganizerProfileService implements OrganizerProfileService {
    private final OrganizerProfileRepository profiles;
    private final Clock clock;

    @Inject
    public DefaultOrganizerProfileService(OrganizerProfileRepository profiles) {
        this(profiles, Clock.systemUTC());
    }

    DefaultOrganizerProfileService(OrganizerProfileRepository profiles, Clock clock) {
        this.profiles = Objects.requireNonNull(profiles, "profiles is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
    }

    @Override
    public Uni<Optional<OrganizerProfile>> findMine(UUID userId) {
        return profiles.findByUserId(Objects.requireNonNull(userId, "userId is required"));
    }

    @Override
    public Uni<OrganizerProfile> saveMine(UUID userId, SaveOrganizerProfileCommand command) {
        UUID ownerId = Objects.requireNonNull(userId, "userId is required");
        OrganizerProfile.SaveFields fields = new OrganizerProfile.SaveFields(command.organizationName(),
                command.contactName(), command.contactEmail(), command.phone(), command.city(), command.state(),
                command.website(), command.description());
        Instant now = clock.instant();
        return profiles.findByUserId(ownerId)
                .map(existing -> existing.map(profile -> {
                    profile.update(fields, now);
                    return profile;
                }).orElseGet(() -> OrganizerProfile.create(ownerId, fields, now)))
                .chain(profiles::upsert);
    }
}
