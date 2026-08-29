package com.squadra.platform.application.services.impl;

import com.squadra.platform.application.services.interfaces.TeamService;
import com.squadra.platform.domain.entities.Team;
import com.squadra.platform.domain.entities.TeamAdministrator;
import com.squadra.platform.domain.enums.TeamAdminRole;
import com.squadra.platform.domain.enums.TeamAthleteMembershipStatus;
import com.squadra.platform.domain.enums.UserRole;
import com.squadra.platform.domain.exceptions.TeamConflictException;
import com.squadra.platform.domain.exceptions.TeamForbiddenException;
import com.squadra.platform.domain.exceptions.TeamNotFoundException;
import com.squadra.platform.domain.exceptions.TeamTargetUserNotFoundException;
import com.squadra.platform.domain.repositories.TeamRepository;
import com.squadra.platform.domain.repositories.UserRepository;
import com.squadra.platform.domain.valueobjects.EmailAddress;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@ApplicationScoped
public final class DefaultTeamService implements TeamService {
    private final TeamRepository teams;
    private final UserRepository users;
    private final Clock clock;

    @Inject
    public DefaultTeamService(TeamRepository teams, UserRepository users) {
        this(teams, users, Clock.systemUTC());
    }

    DefaultTeamService(TeamRepository teams, UserRepository users, Clock clock) {
        this.teams = Objects.requireNonNull(teams, "teams is required");
        this.users = Objects.requireNonNull(users, "users is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
    }

    @Override
    public Uni<TeamRepository.TeamDetails> create(UUID userId, SaveTeamCommand command) {
        UUID ownerId = Objects.requireNonNull(userId, "userId is required");
        Instant now = clock.instant();
        Team team = Team.create(ownerId, fields(command), now);
        return teams.createWithOwner(team, TeamAdministrator.create(team.id(), ownerId, TeamAdminRole.TEAM_OWNER, now));
    }

    @Override
    public Uni<List<TeamRepository.TeamSummary>> listMine(UUID userId) {
        return teams.listForUser(Objects.requireNonNull(userId, "userId is required"));
    }

    @Override
    public Uni<TeamRepository.TeamDetails> getMine(UUID userId, UUID teamId) {
        return visible(teamId, userId);
    }

    @Override
    public Uni<TeamRepository.TeamDetails> update(UUID userId, UUID teamId, SaveTeamCommand command) {
        return visible(teamId, userId).chain(details -> canEdit(details)
                ? saveUpdated(details, userId, command)
                : Uni.createFrom().failure(new TeamForbiddenException()));
    }

    @Override
    public Uni<TeamRepository.TeamDetails> joinAsAthlete(UUID userId, UUID teamId) {
        return visible(teamId, userId).chain(details -> {
            if (details.administrationRole() == null) return Uni.createFrom().failure(new TeamForbiddenException());
            if (details.athleteMembershipStatus() == TeamAthleteMembershipStatus.ACTIVE) return Uni.createFrom().failure(new TeamConflictException());
            return users.findById(userId).chain(user -> user.filter(candidate -> candidate.role() == UserRole.ATHLETE)
                    .map(ignored -> teams.upsertAthleteMembership(teamId, userId, TeamAthleteMembershipStatus.ACTIVE, userId))
                    .orElseGet(() -> Uni.createFrom().failure(new TeamForbiddenException())));
        });
    }

    @Override
    public Uni<TeamRepository.TeamDetails> assignAdmin(UUID userId, UUID teamId, AssignAdminCommand command) {
        if (Objects.requireNonNull(command, "command is required").role() != TeamAdminRole.TEAM_ADMIN) {
            return Uni.createFrom().failure(new IllegalArgumentException("Only TEAM_ADMIN can be assigned here"));
        }
        return requireOwner(userId, teamId).chain(ignored -> users.findByEmail(new EmailAddress(command.email())))
                .chain(target -> target.map(user -> teams.upsertAdmin(teamId, user.id(), TeamAdminRole.TEAM_ADMIN, userId))
                        .orElseGet(() -> Uni.createFrom().failure(new TeamTargetUserNotFoundException())));
    }

    @Override
    public Uni<TeamRepository.TeamDetails> removeAdmin(UUID userId, UUID teamId, UUID targetUserId) {
        return requireOwner(userId, teamId).chain(details -> {
            if (details.team().ownerUserId().equals(targetUserId)) return Uni.createFrom().failure(new IllegalArgumentException("Ownership transfer is required"));
            return teams.removeAdmin(teamId, targetUserId, userId);
        });
    }

    @Override
    public Uni<TeamRepository.TeamDetails> transferOwnership(UUID userId, UUID teamId, UUID targetUserId) {
        return requireOwner(userId, teamId).chain(details -> {
            boolean targetRelated = details.administrators().stream().anyMatch(admin -> admin.userId().equals(targetUserId))
                    || details.athletes().stream().anyMatch(athlete -> athlete.userId().equals(targetUserId)
                    && athlete.status() == TeamAthleteMembershipStatus.ACTIVE);
            if (!targetRelated) return Uni.createFrom().failure(new IllegalArgumentException("Ownership target must have a team relationship"));
            return teams.transferOwnership(teamId, userId, targetUserId);
        });
    }

    @Override
    public Uni<TeamRepository.TeamDetails> inviteAthlete(UUID userId, UUID teamId, InviteAthleteCommand command) {
        return requireAdmin(userId, teamId).chain(details -> users.findByEmail(new EmailAddress(command.email()))
                .chain(target -> target.filter(user -> user.role() == UserRole.ATHLETE)
                        .map(user -> inviteWhenAvailable(details, user.id(), userId))
                        .orElseGet(() -> Uni.createFrom().failure(new TeamTargetUserNotFoundException()))));
    }

    @Override
    public Uni<TeamRepository.TeamDetails> acceptInvitation(UUID userId, UUID teamId) {
        return visible(teamId, userId).chain(details -> {
            if (details.athleteMembershipStatus() == TeamAthleteMembershipStatus.ACTIVE) return Uni.createFrom().failure(new TeamConflictException());
            if (details.athleteMembershipStatus() != TeamAthleteMembershipStatus.INVITED) return Uni.createFrom().failure(new TeamNotFoundException());
            return teams.activateInvitation(teamId, userId);
        });
    }

    @Override
    public Uni<Void> rejectInvitation(UUID userId, UUID teamId) {
        return visible(teamId, userId).chain(details -> {
            if (details.athleteMembershipStatus() == TeamAthleteMembershipStatus.ACTIVE) return Uni.createFrom().failure(new TeamConflictException());
            if (details.athleteMembershipStatus() != TeamAthleteMembershipStatus.INVITED) return Uni.createFrom().failure(new TeamNotFoundException());
            return teams.deleteAthleteMembership(teamId, userId);
        });
    }

    @Override
    public Uni<TeamRepository.TeamDetails> cancelInvitation(UUID userId, UUID teamId, UUID athleteUserId) {
        return requireAdmin(userId, teamId).chain(details -> {
            var athlete = athlete(details, athleteUserId);
            if (athlete == null) return Uni.createFrom().failure(new TeamNotFoundException());
            if (athlete.status() == TeamAthleteMembershipStatus.ACTIVE) return Uni.createFrom().failure(new TeamConflictException());
            return teams.removeAthleteMembership(teamId, athleteUserId, userId);
        });
    }

    @Override
    public Uni<TeamRepository.TeamDetails> removeActiveAthlete(UUID userId, UUID teamId, UUID athleteUserId) {
        return requireAdmin(userId, teamId).chain(details -> {
            var athlete = athlete(details, athleteUserId);
            if (athlete == null) return Uni.createFrom().failure(new TeamNotFoundException());
            if (athlete.status() != TeamAthleteMembershipStatus.ACTIVE) return Uni.createFrom().failure(new TeamNotFoundException());
            return teams.removeAthleteMembership(teamId, athleteUserId, userId);
        });
    }

    private Uni<TeamRepository.TeamDetails> inviteWhenAvailable(TeamRepository.TeamDetails details, UUID athleteUserId, UUID viewerUserId) {
        var existing = athlete(details, athleteUserId);
        if (existing != null) return Uni.createFrom().failure(new TeamConflictException());
        return teams.upsertAthleteMembership(details.team().id(), athleteUserId, TeamAthleteMembershipStatus.INVITED, viewerUserId);
    }

    private Uni<TeamRepository.TeamDetails> saveUpdated(TeamRepository.TeamDetails details, UUID userId, SaveTeamCommand command) {
        details.team().update(fields(command), clock.instant());
        return teams.saveTeam(details.team(), userId);
    }

    private Uni<TeamRepository.TeamDetails> requireOwner(UUID userId, UUID teamId) {
        return visible(teamId, userId).chain(details -> details.administrationRole() == TeamAdminRole.TEAM_OWNER
                ? Uni.createFrom().item(details)
                : Uni.createFrom().failure(new TeamForbiddenException()));
    }

    private Uni<TeamRepository.TeamDetails> requireAdmin(UUID userId, UUID teamId) {
        return visible(teamId, userId).chain(details -> details.administrationRole() != null
                ? Uni.createFrom().item(details)
                : Uni.createFrom().failure(new TeamForbiddenException()));
    }

    private Uni<TeamRepository.TeamDetails> visible(UUID teamId, UUID userId) {
        return teams.findVisibleToUser(Objects.requireNonNull(teamId, "teamId is required"), Objects.requireNonNull(userId, "userId is required"))
                .chain(details -> details.map(Uni.createFrom()::item).orElseGet(() -> Uni.createFrom().failure(new TeamNotFoundException())));
    }

    private static boolean canEdit(TeamRepository.TeamDetails details) {
        return details.administrationRole() == TeamAdminRole.TEAM_OWNER || details.administrationRole() == TeamAdminRole.TEAM_ADMIN;
    }

    private static TeamRepository.AthleteView athlete(TeamRepository.TeamDetails details, UUID athleteUserId) {
        return details.athletes().stream().filter(athlete -> athlete.userId().equals(athleteUserId)).findFirst().orElse(null);
    }

    private static Team.SaveFields fields(SaveTeamCommand command) {
        Objects.requireNonNull(command, "command is required");
        return new Team.SaveFields(command.name(), command.city(), command.state(), command.foundedYear(), command.description());
    }
}
