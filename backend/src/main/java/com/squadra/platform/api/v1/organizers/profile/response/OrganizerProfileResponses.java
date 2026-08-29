package com.squadra.platform.api.v1.organizers.profile.response;

public final class OrganizerProfileResponses {
    private OrganizerProfileResponses() { }

    public record Profile(String id, String organizationName, String contactName, String contactEmail, String phone,
            String city, String state, String website, String description, String createdAt, String updatedAt) { }

    public record Error(String code, String message) { }
}
