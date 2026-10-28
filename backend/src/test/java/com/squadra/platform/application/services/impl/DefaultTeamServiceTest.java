package com.squadra.platform.application.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.squadra.platform.application.services.interfaces.TeamService;
import com.squadra.platform.domain.entities.Team;
import com.squadra.platform.domain.entities.TeamAdministrator;
import com.squadra.platform.domain.entities.User;
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
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DefaultTeamServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-29T10:00:00Z");

    @Test
    void createsTeamWithOwnerAdministrationOnly() {
        var fixtures = fixtures();
        UUID owner = fixtures.user("owner@squadra.test", UserRole.ATHLETE);

        TeamRepository.TeamDetails details = fixtures.service.create(owner, command("Liga Miga")).await().indefinitely();

        assertEquals(owner, details.team().ownerUserId());
        assertEquals(TeamAdminRole.TEAM_OWNER, details.administrationRole());
        assertNull(details.athleteMembershipStatus());
        assertEquals(1, details.administrators().size());
        assertEquals(0, details.athletes().size());
    }

    @Test
    void editsOnlyForAdministratorsAndHidesUnrelatedTeams() {
        var fixtures = fixtures();
        UUID owner = fixtures.user("owner@squadra.test", UserRole.ATHLETE);
        UUID athlete = fixtures.user("athlete@squadra.test", UserRole.ATHLETE);
        UUID unrelated = fixtures.user("other@squadra.test", UserRole.ORGANIZER);
        Team team = fixtures.ownedTeam(owner);
        fixtures.athlete(team.id(), athlete, TeamAthleteMembershipStatus.ACTIVE);

        assertEquals("Updated", fixtures.service.update(owner, team.id(), command("Updated")).await().indefinitely().team().name());
        assertThrows(TeamForbiddenException.class, () -> fixtures.service.update(athlete, team.id(), command("Blocked")).await().indefinitely());
        assertThrows(TeamNotFoundException.class, () -> fixtures.service.getMine(unrelated, team.id()).await().indefinitely());
    }

    @Test
    void supportsSelfJoinOnlyForAthleteAdministrators() {
        var fixtures = fixtures();
        UUID athleteOwner = fixtures.user("owner@squadra.test", UserRole.ATHLETE);
        UUID organizerOwner = fixtures.user("organizer@squadra.test", UserRole.ORGANIZER);
        Team athleteTeam = fixtures.ownedTeam(athleteOwner);
        Team organizerTeam = fixtures.ownedTeam(organizerOwner);

        var joined = fixtures.service.joinAsAthlete(athleteOwner, athleteTeam.id()).await().indefinitely();

        assertEquals(TeamAthleteMembershipStatus.ACTIVE, joined.athleteMembershipStatus());
        assertEquals(TeamAdminRole.TEAM_OWNER, joined.administrationRole());
        assertThrows(TeamConflictException.class, () -> fixtures.service.joinAsAthlete(athleteOwner, athleteTeam.id()).await().indefinitely());
        assertThrows(TeamForbiddenException.class, () -> fixtures.service.joinAsAthlete(organizerOwner, organizerTeam.id()).await().indefinitely());
    }

    @Test
    void managesInvitationsWithDeterministicStates() {
        var fixtures = fixtures();
        UUID owner = fixtures.user("owner@squadra.test", UserRole.ATHLETE);
        UUID athlete = fixtures.user("athlete@squadra.test", UserRole.ATHLETE);
        Team team = fixtures.ownedTeam(owner);

        var invited = fixtures.service.inviteAthlete(owner, team.id(), new TeamService.InviteAthleteCommand("athlete@squadra.test")).await().indefinitely();
        assertEquals(TeamAthleteMembershipStatus.INVITED, invited.athletes().getFirst().status());
        assertThrows(TeamConflictException.class, () -> fixtures.service.inviteAthlete(owner, team.id(), new TeamService.InviteAthleteCommand("athlete@squadra.test")).await().indefinitely());

        var accepted = fixtures.service.acceptInvitation(athlete, team.id()).await().indefinitely();
        assertEquals(TeamAthleteMembershipStatus.ACTIVE, accepted.athleteMembershipStatus());
        assertThrows(TeamConflictException.class, () -> fixtures.service.acceptInvitation(athlete, team.id()).await().indefinitely());
        assertThrows(TeamConflictException.class, () -> fixtures.service.rejectInvitation(athlete, team.id()).await().indefinitely());
    }

    @Test
    void rejectsMissingAndOrganizerInvitationTargets() {
        var fixtures = fixtures();
        UUID owner = fixtures.user("owner@squadra.test", UserRole.ATHLETE);
        fixtures.user("organizer@squadra.test", UserRole.ORGANIZER);
        Team team = fixtures.ownedTeam(owner);

        assertThrows(TeamTargetUserNotFoundException.class, () -> fixtures.service.inviteAthlete(owner, team.id(), new TeamService.InviteAthleteCommand("missing@squadra.test")).await().indefinitely());
        assertThrows(TeamTargetUserNotFoundException.class, () -> fixtures.service.inviteAthlete(owner, team.id(), new TeamService.InviteAthleteCommand("organizer@squadra.test")).await().indefinitely());
    }

    @Test
    void ownerOnlyAdminManagementPreservesAthleteMembership() {
        var fixtures = fixtures();
        UUID owner = fixtures.user("owner@squadra.test", UserRole.ATHLETE);
        UUID admin = fixtures.user("admin@squadra.test", UserRole.ATHLETE);
        UUID activeAdmin = fixtures.user("active-admin@squadra.test", UserRole.ATHLETE);
        Team team = fixtures.ownedTeam(owner);
        fixtures.admin(team.id(), admin, TeamAdminRole.TEAM_ADMIN);
        fixtures.admin(team.id(), activeAdmin, TeamAdminRole.TEAM_ADMIN);
        fixtures.athlete(team.id(), activeAdmin, TeamAthleteMembershipStatus.ACTIVE);

        assertThrows(TeamForbiddenException.class, () -> fixtures.service.assignAdmin(admin, team.id(), new TeamService.AssignAdminCommand("owner@squadra.test", TeamAdminRole.TEAM_ADMIN)).await().indefinitely());
        var removed = fixtures.service.removeAdmin(owner, team.id(), activeAdmin).await().indefinitely();

        assertNull(fixtures.repository.adminRole(team.id(), activeAdmin));
        assertEquals(TeamAthleteMembershipStatus.ACTIVE, removed.athletes().stream().filter(athlete -> athlete.userId().equals(activeAdmin)).findFirst().orElseThrow().status());
    }

    @Test
    void rosterRemovalPreservesAdministrationAndTransferPreservesMembership() {
        var fixtures = fixtures();
        UUID owner = fixtures.user("owner@squadra.test", UserRole.ATHLETE);
        UUID athlete = fixtures.user("athlete@squadra.test", UserRole.ATHLETE);
        Team team = fixtures.ownedTeam(owner);
        fixtures.athlete(team.id(), owner, TeamAthleteMembershipStatus.ACTIVE);
        fixtures.athlete(team.id(), athlete, TeamAthleteMembershipStatus.ACTIVE);

        var transferred = fixtures.service.transferOwnership(owner, team.id(), athlete).await().indefinitely();

        assertEquals(TeamAdminRole.TEAM_ADMIN, transferred.administrationRole());
        assertEquals(TeamAdminRole.TEAM_OWNER, fixtures.repository.adminRole(team.id(), athlete));
        assertEquals(TeamAthleteMembershipStatus.ACTIVE, fixtures.repository.athleteStatus(team.id(), owner));
        var removedSelfRoster = fixtures.service.removeActiveAthlete(owner, team.id(), owner).await().indefinitely();
        assertEquals(TeamAdminRole.TEAM_ADMIN, removedSelfRoster.administrationRole());
        assertNull(fixtures.repository.athleteStatus(team.id(), owner));
    }

    private static TeamService.SaveTeamCommand command(String name) {
        return new TeamService.SaveTeamCommand(name, "Sao Paulo", "SP", 2024, "Training together.");
    }

    private static Fixtures fixtures() {
        var users = new InMemoryUsers();
        var teams = new InMemoryTeams(users);
        return new Fixtures(users, teams, new DefaultTeamService(teams, users, Clock.fixed(NOW, ZoneOffset.UTC)));
    }

    private record Fixtures(InMemoryUsers users, InMemoryTeams repository, DefaultTeamService service) {
        UUID user(String email, UserRole role) {
            UUID id = UUID.randomUUID();
            users.users.put(id, new User(id, new EmailAddress(email), "hashed-password", role, NOW));
            return id;
        }

        Team ownedTeam(UUID ownerId) {
            Team team = Team.create(ownerId, new Team.SaveFields("Liga Miga", "Sao Paulo", "SP", 2024, null), NOW);
            repository.teams.put(team.id(), team);
            admin(team.id(), ownerId, TeamAdminRole.TEAM_OWNER);
            return team;
        }

        void admin(UUID teamId, UUID userId, TeamAdminRole role) { repository.admins.put(new Key(teamId, userId), role); }
        void athlete(UUID teamId, UUID userId, TeamAthleteMembershipStatus status) { repository.athletes.put(new Key(teamId, userId), status); }
    }

    private record Key(UUID teamId, UUID userId) { }

    private static final class InMemoryUsers implements UserRepository {
        final HashMap<UUID, User> users = new HashMap<>();

        @Override
        public Uni<Optional<User>> findByEmail(EmailAddress email) {
            return Uni.createFrom().item(users.values().stream().filter(user -> user.email().equals(email)).findFirst());
        }

        @Override
        public Uni<Optional<User>> findById(UUID id) {
            return Uni.createFrom().item(Optional.ofNullable(users.get(id)));
        }

        @Override
        public Uni<User> save(User user) {
            users.put(user.id(), user);
            return Uni.createFrom().item(user);
        }

        @Override
        public Uni<Void> replacePasswordHash(UUID userId, String passwordHash) {
            users.get(userId).replacePasswordHash(passwordHash);
            return Uni.createFrom().voidItem();
        }
    }

    private static final class InMemoryTeams implements TeamRepository {
        private final InMemoryUsers users;
        final HashMap<UUID, Team> teams = new HashMap<>();
        final HashMap<Key, TeamAdminRole> admins = new HashMap<>();
        final HashMap<Key, TeamAthleteMembershipStatus> athletes = new HashMap<>();

        InMemoryTeams(InMemoryUsers users) { this.users = users; }

        @Override
        public Uni<TeamDetails> createWithOwner(Team team, TeamAdministrator owner) {
            teams.put(team.id(), team);
            admins.put(new Key(team.id(), owner.userId()), owner.role());
            return Uni.createFrom().item(details(team.id(), owner.userId()).orElseThrow());
        }

        @Override
        public Uni<List<TeamSummary>> listForUser(UUID userId) {
            return Uni.createFrom().item(teams.values().stream()
                    .filter(team -> adminRole(team.id(), userId) != null || athleteStatus(team.id(), userId) != null)
                    .map(team -> new TeamSummary(team, adminRole(team.id(), userId), athleteStatus(team.id(), userId))).toList());
        }

        @Override
        public Uni<List<TeamDirectorySummary>> searchDirectory(String query, int limit, int offset) {
            return Uni.createFrom().item(List.of());
        }

        @Override
        public Uni<Boolean> directoryTeamExists(UUID teamId) {
            return Uni.createFrom().item(teams.containsKey(teamId));
        }

        @Override
        public Uni<Optional<TeamDetails>> findVisibleToUser(UUID teamId, UUID userId) {
            return Uni.createFrom().item(details(teamId, userId));
        }

        @Override
        public Uni<TeamDetails> saveTeam(Team team, UUID viewerUserId) {
            teams.put(team.id(), team);
            return Uni.createFrom().item(details(team.id(), viewerUserId).orElseThrow());
        }

        @Override
        public Uni<TeamDetails> upsertAdmin(UUID teamId, UUID userId, TeamAdminRole role, UUID viewerUserId) {
            admins.put(new Key(teamId, userId), role);
            return Uni.createFrom().item(details(teamId, viewerUserId).orElseThrow());
        }

        @Override
        public Uni<TeamDetails> removeAdmin(UUID teamId, UUID userId, UUID viewerUserId) {
            admins.remove(new Key(teamId, userId));
            return Uni.createFrom().item(details(teamId, viewerUserId).orElseThrow());
        }

        @Override
        public Uni<TeamDetails> transferOwnership(UUID teamId, UUID fromUserId, UUID toUserId) {
            teams.get(teamId).transferOwnership(toUserId, NOW);
            admins.put(new Key(teamId, fromUserId), TeamAdminRole.TEAM_ADMIN);
            admins.put(new Key(teamId, toUserId), TeamAdminRole.TEAM_OWNER);
            return Uni.createFrom().item(details(teamId, fromUserId).orElseThrow());
        }

        @Override
        public Uni<TeamDetails> upsertAthleteMembership(UUID teamId, UUID athleteUserId, TeamAthleteMembershipStatus status, UUID viewerUserId) {
            athletes.put(new Key(teamId, athleteUserId), status);
            return Uni.createFrom().item(details(teamId, viewerUserId).orElseThrow());
        }

        @Override
        public Uni<TeamDetails> activateInvitation(UUID teamId, UUID athleteUserId) {
            athletes.put(new Key(teamId, athleteUserId), TeamAthleteMembershipStatus.ACTIVE);
            return Uni.createFrom().item(details(teamId, athleteUserId).orElseThrow());
        }

        @Override
        public Uni<TeamDetails> removeAthleteMembership(UUID teamId, UUID athleteUserId, UUID viewerUserId) {
            athletes.remove(new Key(teamId, athleteUserId));
            return Uni.createFrom().item(details(teamId, viewerUserId).orElseThrow());
        }

        @Override
        public Uni<Void> deleteAthleteMembership(UUID teamId, UUID athleteUserId) {
            athletes.remove(new Key(teamId, athleteUserId));
            return Uni.createFrom().voidItem();
        }

        TeamAdminRole adminRole(UUID teamId, UUID userId) { return admins.get(new Key(teamId, userId)); }
        TeamAthleteMembershipStatus athleteStatus(UUID teamId, UUID userId) { return athletes.get(new Key(teamId, userId)); }

        private Optional<TeamDetails> details(UUID teamId, UUID viewerUserId) {
            Team team = teams.get(teamId);
            if (team == null) return Optional.empty();
            TeamAdminRole adminRole = adminRole(teamId, viewerUserId);
            TeamAthleteMembershipStatus athleteStatus = athleteStatus(teamId, viewerUserId);
            if (adminRole == null && athleteStatus == null) return Optional.empty();
            return Optional.of(new TeamDetails(team, adminRole, athleteStatus, adminViews(teamId), athleteViews(teamId)));
        }

        private List<AdminView> adminViews(UUID teamId) {
            List<AdminView> views = new ArrayList<>();
            admins.forEach((key, role) -> {
                if (key.teamId().equals(teamId)) {
                    User user = users.users.get(key.userId());
                    views.add(new AdminView(key.userId(), user.email().value(), role));
                }
            });
            return views;
        }

        private List<AthleteView> athleteViews(UUID teamId) {
            List<AthleteView> views = new ArrayList<>();
            athletes.forEach((key, status) -> {
                if (key.teamId().equals(teamId)) {
                    User user = users.users.get(key.userId());
                    views.add(new AthleteView(key.userId(), user.email().value(), status));
                }
            });
            return views;
        }
    }
}
