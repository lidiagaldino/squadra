package com.squadra.platform.application.services.interfaces;

import com.squadra.platform.domain.entities.AthleteProfile;
import com.squadra.platform.domain.enums.DominantFoot;
import com.squadra.platform.domain.enums.FootballPosition;
import io.smallrye.mutiny.Uni;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AthleteProfileService {
    Uni<Optional<AthleteProfile>> findMine(UUID userId);
    Uni<AthleteProfile> saveMine(UUID userId, SaveAthleteProfileCommand command);

    record SaveAthleteProfileCommand(String displayName, String fullName, LocalDate birthDate,
            FootballPosition primaryPosition, List<FootballPosition> secondaryPositions, DominantFoot dominantFoot,
            Integer jerseyNumber, String city, String state, String bio) { }
}
