package com.squadra.platform.application.services.impl;

import static org.junit.jupiter.api.Assertions.*;
import com.squadra.platform.domain.exceptions.*;
import com.squadra.platform.domain.repositories.*;
import io.smallrye.mutiny.Uni;
import java.lang.reflect.Proxy;
import java.util.*;
import org.junit.jupiter.api.Test;

class DefaultTournamentInvitationServiceTest {
    private static final UUID ID = UUID.randomUUID();

    @Test
    void lostTeamAdministrationBlocksInboxAndBothDecisions() {
        var service = service();
        assertThrows(TeamForbiddenException.class, () -> service.listTeam(ID, ID).await().indefinitely());
        assertThrows(TeamForbiddenException.class, () -> service.accept(ID, ID, ID).await().indefinitely());
        assertThrows(TeamForbiddenException.class, () -> service.reject(ID, ID, ID).await().indefinitely());
    }

    @Test
    void foreignOwnerCannotInviteListOrCancel() {
        var service = service();
        assertThrows(TournamentNotFoundException.class, () -> service.invite(ID, ID, ID, ID).await().indefinitely());
        assertThrows(TournamentNotFoundException.class, () -> service.listCategory(ID, ID, ID).await().indefinitely());
        assertThrows(TournamentNotFoundException.class, () -> service.cancel(ID, ID, ID, ID).await().indefinitely());
    }

    @Test
    void directoryBoundsProtectRepositoryAndPreserveMinimalResults() {
        var service = service();
        assertThrows(IllegalArgumentException.class, () -> service.searchTeams(-1, 20, "").await().indefinitely());
        assertThrows(IllegalArgumentException.class, () -> service.searchTeams(0, 0, "").await().indefinitely());
        assertThrows(IllegalArgumentException.class, () -> service.searchTeams(0, 51, "").await().indefinitely());
        assertThrows(IllegalArgumentException.class, () -> service.searchTeams(Integer.MAX_VALUE, 20, "").await().indefinitely());
        assertEquals(List.of(), service.searchTeams(0, 20, null).await().indefinitely());
    }

    private static DefaultTournamentInvitationService service() {
        return new DefaultTournamentInvitationService(stub(TournamentInvitationRepository.class), stub(TeamRegistrationRepository.class),
                stub(TeamRepository.class), stub(TournamentRepository.class), stub(TournamentCategoryRepository.class));
    }

    private static <T> T stub(Class<T> type) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> switch (method.getName()) {
            case "findVisibleToUser", "findOwnedByOrganizer" -> Uni.createFrom().item(Optional.empty());
            case "searchDirectory" -> Uni.createFrom().item(List.of());
            default -> throw new AssertionError("Unexpected repository call: " + method.getName());
        }));
    }
}
