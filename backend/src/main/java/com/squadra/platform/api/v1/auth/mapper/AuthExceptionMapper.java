package com.squadra.platform.api.v1.auth.mapper;
import com.squadra.platform.domain.entities.*;
import com.squadra.platform.domain.valueobjects.*;
import com.squadra.platform.domain.enums.*;
import com.squadra.platform.domain.repositories.*;
import com.squadra.platform.domain.services.*;
import com.squadra.platform.domain.exceptions.*;

import com.squadra.platform.domain.exceptions.AuthenticationFailureException;
import com.squadra.platform.domain.exceptions.DuplicateAccountException;
import com.squadra.platform.api.v1.auth.response.AuthResponses;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

/** Maps use-case failures without leaking whether an account or token exists. */
@Provider
public final class AuthExceptionMapper implements ExceptionMapper<RuntimeException> {
    private static final Logger LOG = Logger.getLogger(AuthExceptionMapper.class);

    @Override
    public Response toResponse(RuntimeException exception) {
        if (exception instanceof AuthenticationFailureException) {
            return error(Response.Status.UNAUTHORIZED, "AUTHENTICATION_FAILED", "Authentication failed");
        }
        if (exception instanceof DuplicateAccountException) {
            return error(Response.Status.CONFLICT, "ACCOUNT_ALREADY_EXISTS", "An account with this email already exists");
        }
        if (exception instanceof IllegalArgumentException) {
            return Response.status(422).entity(new AuthResponses.Error("VALIDATION_FAILED", "The request could not be processed")).build();
        }
        LOG.error("Unexpected authentication endpoint failure", exception);
        return error(Response.Status.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred");
    }

    private static Response error(Response.Status status, String code, String message) {
        return Response.status(status).entity(new AuthResponses.Error(code, message)).build();
    }
}
