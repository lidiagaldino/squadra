package com.squadra.platform.api.v1.auth.response;
import com.squadra.platform.domain.entities.*;
import com.squadra.platform.domain.valueobjects.*;
import com.squadra.platform.domain.enums.*;
import com.squadra.platform.domain.repositories.*;
import com.squadra.platform.domain.services.*;
import com.squadra.platform.domain.exceptions.*;

/** HTTP output shapes for authentication; they never contain passwords or opaque tokens. */
public final class AuthResponses {
    private AuthResponses() {
    }

    public record RegisteredUser(String id, String email, String role) {
    }

    public record AccessToken(String accessToken) {
    }
    public record CurrentSession(String id, String email, String role) { }

    public record Error(String code, String message) {
    }
}
