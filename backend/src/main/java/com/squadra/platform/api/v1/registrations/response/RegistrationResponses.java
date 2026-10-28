package com.squadra.platform.api.v1.registrations.response;

import com.squadra.platform.domain.enums.RegistrationStatus;

public final class RegistrationResponses {
    private RegistrationResponses() { }

    public record RegistrationDetails(String id, String teamId, String teamName, String tournamentId,
            String tournamentName, String categoryId, String categoryName, RegistrationStatus status,
            String createdAt, String updatedAt) { }
}
