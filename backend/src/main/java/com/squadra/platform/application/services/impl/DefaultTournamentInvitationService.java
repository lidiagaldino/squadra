package com.squadra.platform.application.services.impl;

import com.squadra.platform.application.services.interfaces.TournamentInvitationService;
import com.squadra.platform.domain.entities.TeamRegistration;
import com.squadra.platform.domain.entities.TournamentInvitation;
import com.squadra.platform.domain.enums.TeamAdminRole;
import com.squadra.platform.domain.enums.TournamentInvitationStatus;
import com.squadra.platform.domain.enums.TournamentStatus;
import com.squadra.platform.domain.enums.TournamentVisibility;
import com.squadra.platform.domain.exceptions.TeamForbiddenException;
import com.squadra.platform.domain.exceptions.TeamRegistrationConflictException;
import com.squadra.platform.domain.exceptions.TournamentCategoryNotFoundException;
import com.squadra.platform.domain.exceptions.TournamentConflictException;
import com.squadra.platform.domain.exceptions.TournamentInvitationConflictException;
import com.squadra.platform.domain.exceptions.TournamentInvitationNotFoundException;
import com.squadra.platform.domain.exceptions.TournamentNotFoundException;
import com.squadra.platform.domain.repositories.TeamRegistrationRepository;
import com.squadra.platform.domain.repositories.TeamRepository;
import com.squadra.platform.domain.repositories.TournamentCategoryRepository;
import com.squadra.platform.domain.repositories.TournamentInvitationRepository;
import com.squadra.platform.domain.repositories.TournamentRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jboss.logging.Logger;

@ApplicationScoped
public final class DefaultTournamentInvitationService implements TournamentInvitationService {
    private static final Logger LOG = Logger.getLogger(DefaultTournamentInvitationService.class);
    private static final int MAX_PAGE_SIZE = 50;
    private final TournamentInvitationRepository invitations;
    private final TeamRegistrationRepository registrations;
    private final TeamRepository teams;
    private final TournamentRepository tournaments;
    private final TournamentCategoryRepository categories;
    private final Clock clock;

    @Inject
    public DefaultTournamentInvitationService(TournamentInvitationRepository invitations, TeamRegistrationRepository registrations,
            TeamRepository teams, TournamentRepository tournaments, TournamentCategoryRepository categories) {
        this(invitations, registrations, teams, tournaments, categories, Clock.systemUTC());
    }

    DefaultTournamentInvitationService(TournamentInvitationRepository invitations, TeamRegistrationRepository registrations,
            TeamRepository teams, TournamentRepository tournaments, TournamentCategoryRepository categories, Clock clock) {
        this.invitations = Objects.requireNonNull(invitations);
        this.registrations = Objects.requireNonNull(registrations);
        this.teams = Objects.requireNonNull(teams);
        this.tournaments = Objects.requireNonNull(tournaments);
        this.categories = Objects.requireNonNull(categories);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public Uni<List<TeamRepository.TeamDirectorySummary>> searchTeams(int page, int size, String query) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE || page > Integer.MAX_VALUE / size) {
            return Uni.createFrom().failure(new IllegalArgumentException("Invalid team page"));
        }
        String normalizedQuery = query == null ? "" : query.trim();
        return teams.searchDirectory(normalizedQuery, size, page * size);
    }

    @Override
    public Uni<TournamentInvitationRepository.InvitationDetails> invite(UUID organizerId, UUID tournamentId,
            UUID categoryId, UUID teamId) {
        Instant now = clock.instant();
        return privateOpenTournament(organizerId, tournamentId)
                .chain(ignored -> category(tournamentId, categoryId))
                .chain(category -> teams.directoryTeamExists(teamId).chain(exists -> {
                    if (!exists) return Uni.createFrom().failure(new TournamentInvitationNotFoundException());
                    return registrations.activeExists(category.id(), teamId).chain(active -> {
                        if (active) return Uni.createFrom().failure(new TeamRegistrationConflictException("Team already has an active registration"));
                        return invitations.create(TournamentInvitation.create(tournamentId, categoryId, teamId, organizerId, now));
                    });
                }))
                .invoke(details -> log("invite", organizerId, details));
    }

    @Override
    public Uni<List<TournamentInvitationRepository.InvitationDetails>> listCategory(UUID organizerId, UUID tournamentId, UUID categoryId) {
        return ownedTournament(organizerId, tournamentId)
                .chain(ignored -> category(tournamentId, categoryId))
                .chain(ignored -> invitations.listByCategory(tournamentId, categoryId));
    }

    @Override
    public Uni<TournamentInvitationRepository.InvitationDetails> cancel(UUID organizerId, UUID tournamentId,
            UUID categoryId, UUID invitationId) {
        return ownedTournament(organizerId, tournamentId)
                .chain(ignored -> findInCategory(tournamentId, categoryId, invitationId))
                .chain(details -> {
                    details.invitation().cancel(clock.instant());
                    return invitations.save(details.invitation());
                })
                .invoke(details -> log("cancel", organizerId, details));
    }

    @Override
    public Uni<List<TournamentInvitationRepository.InvitationDetails>> listTeam(UUID userId, UUID teamId) {
        return requireTeamAdmin(userId, teamId).chain(ignored -> invitations.listByTeam(teamId));
    }

    @Override
    public Uni<TournamentInvitationRepository.InvitationDetails> accept(UUID userId, UUID teamId, UUID invitationId) {
        return requireTeamAdmin(userId, teamId).chain(ignored -> findForTeam(teamId, invitationId))
                .chain(details -> {
                    TournamentInvitation invitation = details.invitation();
                    return openPrivateTournament(invitation.tournamentId())
                            .chain(tournament -> category(invitation.tournamentId(), invitation.categoryId())
                                    .chain(category -> {
                                        invitation.accept(clock.instant());
                                        TeamRegistration registration = TeamRegistration.create(teamId, invitation.tournamentId(),
                                                invitation.categoryId(), clock.instant());
                                        return invitations.acceptWithRegistration(invitation, registration, category.maxTeams());
                                    }));
                })
                .invoke(details -> log("accept", userId, details));
    }

    @Override
    public Uni<TournamentInvitationRepository.InvitationDetails> reject(UUID userId, UUID teamId, UUID invitationId) {
        return requireTeamAdmin(userId, teamId).chain(ignored -> findForTeam(teamId, invitationId))
                .chain(details -> {
                    details.invitation().reject(clock.instant());
                    return invitations.save(details.invitation());
                })
                .invoke(details -> log("reject", userId, details));
    }

    private Uni<TournamentRepository.TournamentDetails> privateOpenTournament(UUID ownerId, UUID tournamentId) {
        return ownedTournament(ownerId, tournamentId).invoke(details -> {
            if (details.tournament().visibility() != TournamentVisibility.PRIVATE) {
                throw new TournamentConflictException("Invitations are only available for private tournaments");
            }
            if (details.tournament().status() != TournamentStatus.OPEN) {
                throw new TournamentConflictException("Tournament is not open for invitations");
            }
        });
    }

    private Uni<TournamentRepository.TournamentDetails> openPrivateTournament(UUID tournamentId) {
        return tournaments.findById(tournamentId).chain(found -> {
            if (found.isEmpty() || found.get().tournament().visibility() != TournamentVisibility.PRIVATE) {
                return Uni.createFrom().failure(new TournamentInvitationNotFoundException());
            }
            if (found.get().tournament().status() != TournamentStatus.OPEN) {
                return Uni.createFrom().failure(new TournamentConflictException("Tournament is not open for invitation acceptance"));
            }
            return Uni.createFrom().item(found.get());
        });
    }

    private Uni<TournamentRepository.TournamentDetails> ownedTournament(UUID userId, UUID tournamentId) {
        return tournaments.findOwnedByOrganizer(tournamentId, userId)
                .chain(found -> found.map(Uni.createFrom()::item)
                        .orElseGet(() -> Uni.createFrom().failure(new TournamentNotFoundException())));
    }

    private Uni<com.squadra.platform.domain.entities.TournamentCategory> category(UUID tournamentId, UUID categoryId) {
        return categories.findByTournament(tournamentId, categoryId)
                .chain(found -> found.map(Uni.createFrom()::item)
                        .orElseGet(() -> Uni.createFrom().failure(new TournamentCategoryNotFoundException())));
    }

    private Uni<TournamentInvitationRepository.InvitationDetails> findForTeam(UUID teamId, UUID invitationId) {
        return invitations.findById(invitationId).chain(found -> found
                .filter(details -> details.invitation().teamId().equals(teamId))
                .map(Uni.createFrom()::item)
                .orElseGet(() -> Uni.createFrom().failure(new TournamentInvitationNotFoundException())));
    }

    private Uni<TournamentInvitationRepository.InvitationDetails> findInCategory(UUID tournamentId, UUID categoryId, UUID invitationId) {
        return category(tournamentId, categoryId).chain(ignored -> invitations.findById(invitationId).chain(found -> found
                .filter(details -> details.invitation().tournamentId().equals(tournamentId)
                        && details.invitation().categoryId().equals(categoryId))
                .map(Uni.createFrom()::item)
                .orElseGet(() -> Uni.createFrom().failure(new TournamentInvitationNotFoundException()))));
    }

    private Uni<TeamRepository.TeamDetails> requireTeamAdmin(UUID userId, UUID teamId) {
        return teams.findVisibleToUser(teamId, userId).chain(found -> {
            if (found.isEmpty() || found.get().administrationRole() == null) return Uni.createFrom().failure(new TeamForbiddenException());
            TeamAdminRole role = found.get().administrationRole();
            return role == TeamAdminRole.TEAM_OWNER || role == TeamAdminRole.TEAM_ADMIN
                    ? Uni.createFrom().item(found.get()) : Uni.createFrom().failure(new TeamForbiddenException());
        });
    }

    private static void log(String action, UUID actor, TournamentInvitationRepository.InvitationDetails details) {
        var invite = details.invitation();
        LOG.infof("tournament.invitation.%s actor=%s team=%s tournament=%s category=%s invitation=%s status=%s",
                action, actor, invite.teamId(), invite.tournamentId(), invite.categoryId(), invite.id(), invite.status());
    }

}
