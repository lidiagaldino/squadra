package com.squadra.platform.api.v1.athletes.profile.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public final class AthleteProfileRequests {
    private AthleteProfileRequests() { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SaveProfile(
            @NotBlank @Size(min = 2, max = 60) String displayName,
            @Size(max = 120) String fullName,
            LocalDate birthDate,
            @NotBlank String primaryPosition,
            List<String> secondaryPositions,
            String dominantFoot,
            @Min(1) @Max(99) Integer jerseyNumber,
            @Size(max = 80) String city,
            @Size(max = 80) String state,
            @Size(max = 280) String bio) { }
}
