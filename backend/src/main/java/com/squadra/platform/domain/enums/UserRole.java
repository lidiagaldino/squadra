package com.squadra.platform.domain.enums;
import com.squadra.platform.domain.entities.*;
import com.squadra.platform.domain.valueobjects.*;
import com.squadra.platform.domain.enums.*;
import com.squadra.platform.domain.repositories.*;
import com.squadra.platform.domain.services.*;
import com.squadra.platform.domain.exceptions.*;

/** Global roles available through self-service registration. */
public enum UserRole {
    ATHLETE,
    ORGANIZER;

    public static UserRole selfServiceValueOf(String value) {
        try {
            return valueOf(value);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new IllegalArgumentException("Role must be ATHLETE or ORGANIZER");
        }
    }
}
