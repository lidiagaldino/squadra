package com.squadra.platform.api.v1.auth;
import com.squadra.platform.api.v1.auth.request.AuthRequests;
import com.squadra.platform.api.v1.auth.response.AuthResponses;
import com.squadra.platform.api.filter.RequiresRoles;
import com.squadra.platform.domain.services.AccessTokenVerifier;
import com.squadra.platform.domain.entities.*;
import com.squadra.platform.domain.valueobjects.*;
import com.squadra.platform.domain.enums.*;
import com.squadra.platform.domain.repositories.*;
import com.squadra.platform.domain.services.*;
import com.squadra.platform.domain.exceptions.*;

import com.squadra.platform.domain.valueobjects.EmailAddress;
import com.squadra.platform.domain.enums.UserRole;
import com.squadra.platform.application.services.interfaces.AuthenticationService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;

/** Versioned HTTP adapter for account access. Authentication rules remain in the service layer. */
@Path("/api/v1/auth")
@Produces(MediaType.APPLICATION_JSON)
public final class AuthResource {
    static final String REFRESH_COOKIE = "squadra_refresh_token";
    private static final int REFRESH_COOKIE_MAX_AGE_SECONDS = 30 * 24 * 60 * 60;

    private final AuthenticationService authentication;

    @Inject
    public AuthResource(AuthenticationService authentication) {
        this.authentication = authentication;
    }

    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<Response> register(@Valid AuthRequests.Register request) {
        return authentication.register(new AuthenticationService.RegisterCommand(
                        new EmailAddress(request.email()), request.password(), UserRole.selfServiceValueOf(request.role())))
                .map(user -> Response.status(Response.Status.CREATED)
                        .entity(new AuthResponses.RegisteredUser(user.id(), user.email().value(), user.role().name()))
                        .build());
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<Response> login(@Valid AuthRequests.Login request) {
        return authentication.login(new AuthenticationService.LoginCommand(new EmailAddress(request.email()), request.password()))
                .map(this::sessionResponse);
    }

    @GET @Path("/session") @RequiresRoles({UserRole.ATHLETE, UserRole.ORGANIZER})
    public AuthResponses.CurrentSession session(@Context ContainerRequestContext request) {
        var token = (AccessTokenVerifier.VerifiedAccessToken) request.getProperty("access-token");
        return new AuthResponses.CurrentSession(token.userId().toString(), token.email().value(), token.role().name());
    }

    @POST
    @Path("/refresh")
    public Uni<Response> refresh(@CookieParam(REFRESH_COOKIE) String refreshToken) {
        return authentication.refresh(refreshToken).map(this::sessionResponse);
    }

    @POST
    @Path("/logout")
    public Uni<Response> logout(@CookieParam(REFRESH_COOKIE) String refreshToken) {
        return authentication.logout(refreshToken)
                .replaceWith(() -> Response.noContent().cookie(clearedRefreshCookie()).build());
    }

    @POST
    @Path("/password-recovery")
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<Response> requestPasswordRecovery(@Valid AuthRequests.PasswordRecovery request) {
        return authentication.requestPasswordRecovery(new EmailAddress(request.email()))
                .replaceWith(() -> Response.accepted().build());
    }

    @POST
    @Path("/password-reset")
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<Response> resetPassword(@Valid AuthRequests.PasswordReset request) {
        return authentication.resetPassword(new AuthenticationService.ResetPasswordCommand(request.token(), request.password()))
                .replaceWith(() -> Response.noContent().build());
    }

    private Response sessionResponse(AuthenticationService.AuthenticatedSession session) {
        return Response.ok(new AuthResponses.AccessToken(session.accessToken()))
                .cookie(refreshCookie(session.refreshToken()))
                .build();
    }

    private static NewCookie refreshCookie(String value) {
        return new NewCookie.Builder(REFRESH_COOKIE)
                .value(value)
                .path("/api/v1/auth")
                .maxAge(REFRESH_COOKIE_MAX_AGE_SECONDS)
                .secure(true)
                .httpOnly(true)
                .sameSite(NewCookie.SameSite.STRICT)
                .build();
    }

    private static NewCookie clearedRefreshCookie() {
        return new NewCookie.Builder(REFRESH_COOKIE)
                .value("")
                .path("/api/v1/auth")
                .maxAge(0)
                .secure(true)
                .httpOnly(true)
                .sameSite(NewCookie.SameSite.STRICT)
                .build();
    }
}
