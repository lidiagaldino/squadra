package com.squadra.platform.api.v1.registrations.response;

import com.squadra.platform.domain.enums.TournamentInvitationStatus;

public final class InvitationResponses {
    private InvitationResponses() { }

    public record InvitationDetails(String id, String tournamentId, String tournamentName, String categoryId,
            String categoryName, String teamId, String teamName, TournamentInvitationStatus status,
            String createdAt, String updatedAt, String decidedAt) { }

    public record TeamDirectoryItem(String id, String name, String city, String state) { }
}
