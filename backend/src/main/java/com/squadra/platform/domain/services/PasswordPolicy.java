package com.squadra.platform.domain.services;
import com.squadra.platform.domain.entities.*;
import com.squadra.platform.domain.valueobjects.*;
import com.squadra.platform.domain.enums.*;
import com.squadra.platform.domain.repositories.*;
import com.squadra.platform.domain.services.*;
import com.squadra.platform.domain.exceptions.*;

import java.util.Objects;

/** Keeps password policy at the domain boundary, before any value is hashed. */
public final class PasswordPolicy {
    public static final int MINIMUM_LENGTH = 12;
    public static final int MAXIMUM_LENGTH = 72;

    private PasswordPolicy() {
    }

    public static void validate(String password) {
        Objects.requireNonNull(password, "password is required");
        if (password.length() < MINIMUM_LENGTH || password.length() > MAXIMUM_LENGTH) {
            throw new IllegalArgumentException("Password must contain between 12 and 72 characters");
        }
    }
}
