package com.squadra.platform.application.services.interfaces;
import com.squadra.platform.domain.entities.*;
import com.squadra.platform.domain.valueobjects.*;
import com.squadra.platform.domain.enums.*;
import com.squadra.platform.domain.repositories.*;
import com.squadra.platform.domain.services.*;
import com.squadra.platform.domain.exceptions.*;

import com.squadra.platform.domain.valueobjects.EmailAddress;
import com.squadra.platform.domain.enums.UserRole;
import io.smallrye.mutiny.Uni;

public interface AuthenticationService {
    Uni<RegisteredUser> register(RegisterCommand command);
    Uni<AuthenticatedSession> login(LoginCommand command);
    Uni<AuthenticatedSession> refresh(String refreshToken);
    Uni<Void> logout(String refreshToken);
    Uni<Void> requestPasswordRecovery(EmailAddress email);
    Uni<Void> resetPassword(ResetPasswordCommand command);

    record RegisterCommand(EmailAddress email, String password, UserRole role) { }
    record LoginCommand(EmailAddress email, String password) { }
    record ResetPasswordCommand(String token, String password) { }
    record RegisteredUser(String id, EmailAddress email, UserRole role) { }
    record AuthenticatedSession(String accessToken, String refreshToken) { }
}
