package com.squadra.platform.api.v1.registrations.mapper;

import com.squadra.platform.domain.exceptions.TeamRegistrationConflictException;
import com.squadra.platform.domain.exceptions.TeamRegistrationNotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;

public final class TeamRegistrationExceptionMapper {
    private TeamRegistrationExceptionMapper() { }

    @Provider
    public static final class NotFound implements ExceptionMapper<TeamRegistrationNotFoundException> {
        @Override public Response toResponse(TeamRegistrationNotFoundException exception) {
            return Response.status(404).entity(Map.of("code", "TEAM_REGISTRATION_NOT_FOUND")).build();
        }
    }

    @Provider
    public static final class Conflict implements ExceptionMapper<TeamRegistrationConflictException> {
        @Override public Response toResponse(TeamRegistrationConflictException exception) {
            return Response.status(409).entity(Map.of("code", "TEAM_REGISTRATION_CONFLICT")).build();
        }
    }
}
