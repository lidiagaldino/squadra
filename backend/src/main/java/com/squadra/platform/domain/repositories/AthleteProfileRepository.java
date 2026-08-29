package com.squadra.platform.domain.repositories;

import com.squadra.platform.domain.entities.AthleteProfile;
import io.smallrye.mutiny.Uni;
import java.util.Optional;
import java.util.UUID;

public interface AthleteProfileRepository {
    Uni<Optional<AthleteProfile>> findByUserId(UUID userId);
    Uni<AthleteProfile> upsert(AthleteProfile profile);
}
