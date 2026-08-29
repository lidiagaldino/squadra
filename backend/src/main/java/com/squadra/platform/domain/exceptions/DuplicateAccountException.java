package com.squadra.platform.domain.exceptions;
import com.squadra.platform.domain.entities.*;
import com.squadra.platform.domain.valueobjects.*;
import com.squadra.platform.domain.enums.*;
import com.squadra.platform.domain.repositories.*;
import com.squadra.platform.domain.services.*;
import com.squadra.platform.domain.exceptions.*;

/** Raised when self-service registration conflicts with an existing account. */
public final class DuplicateAccountException extends RuntimeException {
    public DuplicateAccountException() {
        super("An account with this email already exists");
    }
}
