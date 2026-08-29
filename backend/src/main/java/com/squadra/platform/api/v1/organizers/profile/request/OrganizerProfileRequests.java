package com.squadra.platform.api.v1.organizers.profile.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class OrganizerProfileRequests {
    private OrganizerProfileRequests() { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SaveProfile(
            @NotBlank @Size(min = 2, max = 100) String organizationName,
            @NotBlank @Size(min = 2, max = 100) String contactName,
            @NotBlank @Email @Size(max = 320) String contactEmail,
            @Size(max = 30) String phone,
            @NotBlank @Size(min = 2, max = 80) String city,
            @NotBlank @Size(min = 2, max = 80) String state,
            @Size(max = 200) @Pattern(regexp = "^$|^https?://.*$") String website,
            @Size(max = 500) String description) { }
}
