package com.squadra.platform.api.v1.tournaments.response;

import com.squadra.platform.domain.enums.TournamentStatus;

public final class TournamentResponses {
    private TournamentResponses() { }

    public record TournamentSummary(String id, String ownerOrganizerUserId, TournamentStatus status, String name,
            String description, String city, String state, String venueName, String startDate, String endDate,
            String registrationDeadline, String rules, int categoryCount, String publishedAt, String closedAt,
            String cancelledAt, String createdAt, String updatedAt) { }

    public record TournamentDetails(String id, String ownerOrganizerUserId, TournamentStatus status, String name,
            String description, String city, String state, String venueName, String startDate, String endDate,
            String registrationDeadline, String rules, int categoryCount, String publishedAt, String closedAt,
            String cancelledAt, String createdAt, String updatedAt) { }
}
