package com.squadra.platform.api.v1.auth.mapper;
import com.squadra.platform.domain.entities.*;
import com.squadra.platform.domain.valueobjects.*;
import com.squadra.platform.domain.enums.*;
import com.squadra.platform.domain.repositories.*;
import com.squadra.platform.domain.services.*;
import com.squadra.platform.domain.exceptions.*;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Keeps malformed authentication input within the API's 422 validation contract. */
@Provider
public final class AuthValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {
    @Override
    public Response toResponse(ConstraintViolationException exception) {
        return Response.status(422)
                .entity(new com.squadra.platform.api.v1.auth.response.AuthResponses.Error("VALIDATION_FAILED", "The request could not be processed"))
                .build();
    }
}
