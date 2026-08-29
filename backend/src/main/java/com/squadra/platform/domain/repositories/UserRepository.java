package com.squadra.platform.domain.repositories;
import com.squadra.platform.domain.entities.*;
import com.squadra.platform.domain.valueobjects.*;
import com.squadra.platform.domain.enums.*;
import com.squadra.platform.domain.repositories.*;
import com.squadra.platform.domain.services.*;
import com.squadra.platform.domain.exceptions.*;

import io.smallrye.mutiny.Uni;
import java.util.Optional;
import java.util.UUID;

/** Persistence port; its implementation belongs in infra. */
public interface UserRepository {
    Uni<Optional<User>> findByEmail(EmailAddress email);
    Uni<Optional<User>> findById(UUID id);
    Uni<User> save(User user);
    Uni<Void> replacePasswordHash(UUID userId, String passwordHash);
}
