package com.squadra.platform.api.v1.auth.request;
import com.squadra.platform.domain.entities.*;
import com.squadra.platform.domain.valueobjects.*;
import com.squadra.platform.domain.enums.*;
import com.squadra.platform.domain.repositories.*;
import com.squadra.platform.domain.services.*;
import com.squadra.platform.domain.exceptions.*;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** HTTP input shapes for authentication. Secrets are deliberately request-only. */
public final class AuthRequests {
    private AuthRequests() {
    }

    public record Register(
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(min = 12, max = 72) String password,
            @NotNull @Pattern(regexp = "ATHLETE|ORGANIZER") String role) {
    }

    public record Login(
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(max = 72) String password) {
    }

    public record PasswordRecovery(@NotBlank @Email @Size(max = 320) String email) {
    }

    public record PasswordReset(
            @NotBlank @Size(max = 512) String token,
            @NotBlank @Size(min = 12, max = 72) String password) {
    }
}
