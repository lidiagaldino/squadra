package com.squadra.platform.api.v1.teams.mapper;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.squadra.platform.api.v1.teams.response.TeamResponses;
import jakarta.annotation.Priority;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(1)
public final class TeamJsonMappingExceptionMapper implements ExceptionMapper<MismatchedInputException> {
    @Override
    public Response toResponse(MismatchedInputException exception) {
        return Response.status(422)
                .entity(new TeamResponses.Error("VALIDATION_FAILED", "The request could not be processed"))
                .build();
    }
}
