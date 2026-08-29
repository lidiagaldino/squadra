package com.squadra.platform.application.services.impl;

import com.squadra.platform.application.services.interfaces.AthleteProfileService;
import com.squadra.platform.domain.entities.AthleteProfile;
import com.squadra.platform.domain.repositories.AthleteProfileRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public final class DefaultAthleteProfileService implements AthleteProfileService {
    private final AthleteProfileRepository profiles;
    private final Clock clock;

    @Inject
    public DefaultAthleteProfileService(AthleteProfileRepository profiles) {
        this(profiles, Clock.systemUTC());
    }

    DefaultAthleteProfileService(AthleteProfileRepository profiles, Clock clock) {
        this.profiles = Objects.requireNonNull(profiles, "profiles is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
    }

    @Override
    public Uni<Optional<AthleteProfile>> findMine(UUID userId) {
        return profiles.findByUserId(Objects.requireNonNull(userId, "userId is required"));
    }

    @Override
    public Uni<AthleteProfile> saveMine(UUID userId, SaveAthleteProfileCommand command) {
        UUID ownerId = Objects.requireNonNull(userId, "userId is required");
        AthleteProfile.SaveFields fields = new AthleteProfile.SaveFields(command.displayName(), command.fullName(),
                command.birthDate(), command.primaryPosition(), secondaryPositions(command), command.dominantFoot(),
                command.jerseyNumber(), command.city(), command.state(), command.bio());
        Instant now = clock.instant();
        return profiles.findByUserId(ownerId)
                .map(existing -> existing.map(profile -> {
                    profile.update(fields, now);
                    return profile;
                }).orElseGet(() -> AthleteProfile.create(ownerId, fields, now)))
                .chain(profiles::upsert);
    }

    private static EnumSet<com.squadra.platform.domain.enums.FootballPosition> secondaryPositions(SaveAthleteProfileCommand command) {
        if (command.secondaryPositions() == null || command.secondaryPositions().isEmpty()) {
            return EnumSet.noneOf(com.squadra.platform.domain.enums.FootballPosition.class);
        }
        return EnumSet.copyOf(command.secondaryPositions());
    }
}
