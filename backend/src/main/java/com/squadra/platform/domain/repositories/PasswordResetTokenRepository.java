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

public interface PasswordResetTokenRepository {
    Uni<Optional<PasswordResetToken>> findByTokenHash(String tokenHash);
    Uni<PasswordResetToken> save(PasswordResetToken token);
    Uni<Void> consume(UUID tokenId, Instant usedAt);
}
