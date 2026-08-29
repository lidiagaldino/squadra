package com.squadra.platform.domain.services;
import com.squadra.platform.domain.entities.*;
import com.squadra.platform.domain.valueobjects.*;
import com.squadra.platform.domain.enums.*;
import com.squadra.platform.domain.repositories.*;
import com.squadra.platform.domain.services.*;
import com.squadra.platform.domain.exceptions.*;

import java.util.UUID;

/** Verifies access tokens before API authorization decisions are made. */
public interface AccessTokenVerifier {
    VerifiedAccessToken verify(String rawToken);

    record VerifiedAccessToken(UUID userId, EmailAddress email, UserRole role) { }
}
