package com.squadra.platform.domain.exceptions;
import com.squadra.platform.domain.entities.*;
import com.squadra.platform.domain.valueobjects.*;
import com.squadra.platform.domain.enums.*;
import com.squadra.platform.domain.repositories.*;
import com.squadra.platform.domain.services.*;
import com.squadra.platform.domain.exceptions.*;

/**
 * Deliberately generic authentication failure. API adapters must not turn this
 * into account- or token-specific messages.
 */
public final class AuthenticationFailureException extends RuntimeException {
    public AuthenticationFailureException() {
        super("Authentication failed");
    }
}
