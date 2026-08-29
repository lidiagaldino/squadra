package com.squadra.platform.api.v1.teams.response;

import com.squadra.platform.domain.enums.TeamAdminRole;
import com.squadra.platform.domain.enums.TeamAthleteMembershipStatus;
import java.util.List;

public final class TeamResponses {
    private TeamResponses() { }

    public record TeamSummary(String id, String name, String city, String state, Integer foundedYear,
            String description, TeamAdminRole administrationRole,
            TeamAthleteMembershipStatus athleteMembershipStatus, String createdAt, String updatedAt) { }

    public record TeamDetails(String id, String name, String city, String state, Integer foundedYear,
            String description, TeamAdminRole administrationRole,
            TeamAthleteMembershipStatus athleteMembershipStatus, List<Administrator> administrators,
            List<Athlete> athletes, String createdAt, String updatedAt) { }

    public record Administrator(String userId, String email, TeamAdminRole role) { }
    public record Athlete(String userId, String email, TeamAthleteMembershipStatus status) { }
    public record Error(String code, String message) { }
}
