package com.squadra.platform.domain.repositories;
import com.squadra.platform.domain.entities.*;
import com.squadra.platform.domain.valueobjects.*;
import com.squadra.platform.domain.enums.*;
import com.squadra.platform.domain.repositories.*;
import com.squadra.platform.domain.services.*;
import com.squadra.platform.domain.exceptions.*;

import io.smallrye.mutiny.Uni;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshSessionRepository {
    Uni<Optional<RefreshSession>> findByTokenHash(String tokenHash);
    Uni<RefreshSession> save(RefreshSession session);
    Uni<Void> revokeById(UUID sessionId, Instant revokedAt);
    Uni<Void> revokeAllForUser(UUID userId, Instant revokedAt);
}
