package com.squadra.platform.application.services.impl;

import com.squadra.platform.application.services.interfaces.TeamRegistrationService;
import com.squadra.platform.domain.entities.TeamRegistration;
import com.squadra.platform.domain.entities.TournamentCategory;
import com.squadra.platform.domain.enums.TeamAdminRole;
import com.squadra.platform.domain.enums.TournamentStatus;
import com.squadra.platform.domain.enums.TournamentVisibility;
import com.squadra.platform.domain.exceptions.TeamForbiddenException;
import com.squadra.platform.domain.exceptions.TeamRegistrationNotFoundException;
import com.squadra.platform.domain.exceptions.TournamentCategoryNotFoundException;
import com.squadra.platform.domain.exceptions.TournamentConflictException;
import com.squadra.platform.domain.exceptions.TournamentNotFoundException;
import com.squadra.platform.domain.repositories.TeamRegistrationRepository;
import com.squadra.platform.domain.repositories.TeamRepository;
import com.squadra.platform.domain.repositories.TournamentCategoryRepository;
import com.squadra.platform.domain.repositories.TournamentRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jboss.logging.Logger;

@ApplicationScoped
public final class DefaultTeamRegistrationService implements TeamRegistrationService {
    private static final Logger LOG = Logger.getLogger(DefaultTeamRegistrationService.class);

    private final TeamRegistrationRepository registrations;
    private final TeamRepository teams;
    private final TournamentRepository tournaments;
    private final TournamentCategoryRepository categories;
    private final Clock clock;

    @Inject
    public DefaultTeamRegistrationService(TeamRegistrationRepository registrations, TeamRepository teams,
            TournamentRepository tournaments, TournamentCategoryRepository categories) {
        this(registrations, teams, tournaments, categories, Clock.systemUTC());
    }

    DefaultTeamRegistrationService(TeamRegistrationRepository registrations, TeamRepository teams,
            TournamentRepository tournaments, TournamentCategoryRepository categories, Clock clock) {
        this.registrations = Objects.requireNonNull(registrations, "registrations is required");
        this.teams = Objects.requireNonNull(teams, "teams is required");
        this.tournaments = Objects.requireNonNull(tournaments, "tournaments is required");
        this.categories = Objects.requireNonNull(categories, "categories is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
    }

    @Override
    public Uni<TeamRegistrationRepository.RegistrationDetails> register(UUID userId, UUID tournamentId, UUID categoryId,
            RegisterTeamCommand command) {
        UUID actorId = Objects.requireNonNull(userId, "userId is required");
        UUID routeTournamentId = Objects.requireNonNull(tournamentId, "tournamentId is required");
        UUID routeCategoryId = Objects.requireNonNull(categoryId, "categoryId is required");
        UUID teamId = Objects.requireNonNull(Objects.requireNonNull(command, "command is required").teamId(), "teamId is required");
        return requireTeamAdmin(actorId, teamId)
                .chain(ignored -> category(routeTournamentId, routeCategoryId))
                .chain(category -> openTournament(routeTournamentId).chain(tournament ->
                        tournament.tournament().visibility() == TournamentVisibility.PUBLIC
                                ? Uni.createFrom().item(category)
                                : Uni.createFrom().failure(new TournamentNotFoundException())))
                .chain(category -> registrations.createWithinCapacity(
                        TeamRegistration.create(teamId, routeTournamentId, category.id(), clock.instant()), category.maxTeams()))
                .invoke(details -> log("create", actorId, details));
    }

    @Override
    public Uni<List<TeamRegistrationRepository.RegistrationDetails>> listCategory(UUID userId, UUID tournamentId, UUID categoryId) {
        UUID ownerId = Objects.requireNonNull(userId, "userId is required");
        UUID routeTournamentId = Objects.requireNonNull(tournamentId, "tournamentId is required");
        UUID routeCategoryId = Objects.requireNonNull(categoryId, "categoryId is required");
        return tournaments.findOwnedByOrganizer(routeTournamentId, ownerId)
                .chain(details -> details.map(Uni.createFrom()::item)
                        .orElseGet(() -> Uni.createFrom().failure(new TournamentNotFoundException())))
                .chain(ignored -> category(routeTournamentId, routeCategoryId))
                .chain(ignored -> registrations.listByCategory(routeTournamentId, routeCategoryId));
    }

    @Override
    public Uni<List<TeamRegistrationRepository.RegistrationDetails>> listTeam(UUID userId, UUID teamId) {
        UUID actorId = Objects.requireNonNull(userId, "userId is required");
        UUID routeTeamId = Objects.requireNonNull(teamId, "teamId is required");
        return requireTeamAdmin(actorId, routeTeamId).chain(ignored -> registrations.listByTeam(routeTeamId));
    }

    @Override
    public Uni<TeamRegistrationRepository.RegistrationDetails> cancelTeamRegistration(UUID userId, UUID teamId, UUID registrationId) {
        UUID actorId = Objects.requireNonNull(userId, "userId is required");
        UUID routeTeamId = Objects.requireNonNull(teamId, "teamId is required");
        UUID routeRegistrationId = Objects.requireNonNull(registrationId, "registrationId is required");
        return requireTeamAdmin(actorId, routeTeamId)
                .chain(ignored -> registrations.findByTeam(routeTeamId, routeRegistrationId))
                .chain(found -> found.map(Uni.createFrom()::item)
                        .orElseGet(() -> Uni.createFrom().failure(new TeamRegistrationNotFoundException())))
                .chain(details -> {
                    details.registration().cancel(clock.instant());
                    return registrations.save(details.registration());
                })
                .invoke(details -> log("cancel", actorId, details));
    }

    private Uni<TeamRepository.TeamDetails> requireTeamAdmin(UUID userId, UUID teamId) {
        return teams.findVisibleToUser(teamId, userId).chain(found -> {
            if (found.isEmpty() || found.get().administrationRole() == null) {
                return Uni.createFrom().failure(new TeamForbiddenException());
            }
            TeamAdminRole role = found.get().administrationRole();
            return role == TeamAdminRole.TEAM_OWNER || role == TeamAdminRole.TEAM_ADMIN
                    ? Uni.createFrom().item(found.get())
                    : Uni.createFrom().failure(new TeamForbiddenException());
        });
    }

    private Uni<TournamentRepository.TournamentDetails> openTournament(UUID tournamentId) {
        return tournaments.findById(tournamentId).chain(found -> {
            if (found.isEmpty()) return Uni.createFrom().failure(new TournamentNotFoundException());
            if (found.get().tournament().status() != TournamentStatus.OPEN) {
                return Uni.createFrom().failure(new TournamentConflictException("Tournament is not open for registration"));
            }
            return Uni.createFrom().item(found.get());
        });
    }

    private Uni<TournamentCategory> category(UUID tournamentId, UUID categoryId) {
        return categories.findByTournament(tournamentId, categoryId).chain(found -> found.map(Uni.createFrom()::item)
                .orElseGet(() -> Uni.createFrom().failure(new TournamentCategoryNotFoundException())));
    }

    private static void log(String action, UUID actorId, TeamRegistrationRepository.RegistrationDetails details) {
        TeamRegistration registration = details.registration();
        LOG.infof("team.registration.%s actor=%s team=%s tournament=%s category=%s registration=%s status=%s",
                action, actorId, registration.teamId(), registration.tournamentId(), registration.categoryId(),
                registration.id(), registration.status());
    }
}
