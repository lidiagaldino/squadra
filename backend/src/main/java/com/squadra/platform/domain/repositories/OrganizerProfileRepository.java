package com.squadra.platform.domain.repositories;

import com.squadra.platform.domain.entities.OrganizerProfile;
import io.smallrye.mutiny.Uni;
import java.util.Optional;
import java.util.UUID;

public interface OrganizerProfileRepository {
    Uni<Optional<OrganizerProfile>> findByUserId(UUID userId);
    Uni<OrganizerProfile> upsert(OrganizerProfile profile);
}
