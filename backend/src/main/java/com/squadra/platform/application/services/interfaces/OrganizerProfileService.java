package com.squadra.platform.application.services.interfaces;

import com.squadra.platform.domain.entities.OrganizerProfile;
import io.smallrye.mutiny.Uni;
import java.util.Optional;
import java.util.UUID;

public interface OrganizerProfileService {
    Uni<Optional<OrganizerProfile>> findMine(UUID userId);
    Uni<OrganizerProfile> saveMine(UUID userId, SaveOrganizerProfileCommand command);

    record SaveOrganizerProfileCommand(String organizationName, String contactName, String contactEmail, String phone,
            String city, String state, String website, String description) { }
}
