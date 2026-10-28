package com.squadra.platform.application.services.impl;

import com.squadra.platform.application.services.interfaces.TournamentDiscoveryService;
import com.squadra.platform.domain.enums.UserRole;
import com.squadra.platform.domain.exceptions.TournamentNotFoundException;
import com.squadra.platform.domain.repositories.TournamentCategoryRepository;
import com.squadra.platform.domain.repositories.TournamentInvitationRepository;
import com.squadra.platform.domain.repositories.TournamentRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@ApplicationScoped
public final class DefaultTournamentDiscoveryService implements TournamentDiscoveryService {
    private static final int MAX_PAGE_SIZE = 50;

    private final TournamentRepository tournaments;
    private final TournamentCategoryRepository categories;
    private final TournamentInvitationRepository invitations;

    @Inject
    public DefaultTournamentDiscoveryService(TournamentRepository tournaments, TournamentCategoryRepository categories,
            TournamentInvitationRepository invitations) {
        this.tournaments = Objects.requireNonNull(tournaments, "tournaments is required");
        this.categories = Objects.requireNonNull(categories, "categories is required");
        this.invitations = Objects.requireNonNull(invitations, "invitations is required");
    }

    @Override
    public Uni<List<TournamentRepository.TournamentSummary>> available(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE || page > Integer.MAX_VALUE / size) {
            return Uni.createFrom().failure(new IllegalArgumentException("Invalid tournament page"));
        }
        return tournaments.listAvailable(size, page * size);
    }

    @Override
    public Uni<TournamentBrowseDetails> overview(UUID userId, UUID tournamentId) {
        UUID actorId = Objects.requireNonNull(userId, "userId is required");
        UUID routeTournamentId = Objects.requireNonNull(tournamentId, "tournamentId is required");
        return tournaments.findOwnedByOrganizer(routeTournamentId, actorId).chain(owned -> {
            if (owned.isPresent()) return withCategories(routeTournamentId, owned.get());
            return tournaments.findPublicOpen(routeTournamentId).chain(found -> {
                if (found.isPresent()) return withCategories(routeTournamentId, found.get());
                return invitations.hasTeamAdminAccess(routeTournamentId, actorId).chain(hasAccess -> {
                    if (!hasAccess) return Uni.createFrom().failure(new TournamentNotFoundException());
                    return tournaments.findById(routeTournamentId)
                            .chain(privateTournament -> privateTournament.map(details -> withCategories(routeTournamentId, details))
                                    .orElseGet(() -> Uni.createFrom().failure(new TournamentNotFoundException())));
                });
            });
        });
    }

    private Uni<TournamentBrowseDetails> withCategories(UUID tournamentId, TournamentRepository.TournamentDetails details) {
        return categories.listByTournament(tournamentId)
                .map(items -> new TournamentBrowseDetails(details, items));
    }
}
