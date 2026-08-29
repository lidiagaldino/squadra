package com.squadra.platform.api.v1.athletes.profile.response;

import java.util.List;

public final class AthleteProfileResponses {
    private AthleteProfileResponses() { }

    public record Profile(String id, String displayName, String fullName, String birthDate, String primaryPosition,
            List<String> secondaryPositions, String dominantFoot, Integer jerseyNumber, String city, String state,
            String bio, String createdAt, String updatedAt) { }

    public record Error(String code, String message) { }
}
