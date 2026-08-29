package com.squadra.platform.api.v1.teams.mapper;

import com.squadra.platform.domain.exceptions.TeamConflictException;
import com.squadra.platform.domain.exceptions.TeamForbiddenException;
import com.squadra.platform.domain.exceptions.TeamNotFoundException;
import com.squadra.platform.domain.exceptions.TeamTargetUserNotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;

public final class TeamExceptionMapper {
    private TeamExceptionMapper() { }

    @Provider
    public static final class NotFound implements ExceptionMapper<TeamNotFoundException> {
        @Override public Response toResponse(TeamNotFoundException exception) {
            return Response.status(404).entity(Map.of("code", "TEAM_NOT_FOUND")).build();
        }
    }

    @Provider
    public static final class TargetNotFound implements ExceptionMapper<TeamTargetUserNotFoundException> {
        @Override public Response toResponse(TeamTargetUserNotFoundException exception) {
            return Response.status(404).entity(Map.of("code", "TEAM_TARGET_USER_NOT_FOUND")).build();
        }
    }

    @Provider
    public static final class Forbidden implements ExceptionMapper<TeamForbiddenException> {
        @Override public Response toResponse(TeamForbiddenException exception) {
            return Response.status(403).entity(Map.of("code", "TEAM_FORBIDDEN")).build();
        }
    }

    @Provider
    public static final class Conflict implements ExceptionMapper<TeamConflictException> {
        @Override public Response toResponse(TeamConflictException exception) {
            return Response.status(409).entity(Map.of("code", "TEAM_MEMBERSHIP_CONFLICT")).build();
        }
    }

    @Provider
    public static final class Invalid implements ExceptionMapper<IllegalArgumentException> {
        @Override public Response toResponse(IllegalArgumentException exception) {
            return Response.status(422).entity(Map.of("code", "VALIDATION_FAILED")).build();
        }
    }
}
