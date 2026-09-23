package com.squadra.platform.api.v1.tournaments.mapper;

import com.squadra.platform.domain.exceptions.TournamentConflictException;
import com.squadra.platform.domain.exceptions.TournamentNotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;

public final class TournamentExceptionMapper {
    private TournamentExceptionMapper() { }

    @Provider
    public static final class NotFound implements ExceptionMapper<TournamentNotFoundException> {
        @Override public Response toResponse(TournamentNotFoundException exception) {
            return Response.status(404).entity(Map.of("code", "TOURNAMENT_NOT_FOUND")).build();
        }
    }

    @Provider
    public static final class Conflict implements ExceptionMapper<TournamentConflictException> {
        @Override public Response toResponse(TournamentConflictException exception) {
            return Response.status(409).entity(Map.of("code", "TOURNAMENT_STATE_CONFLICT")).build();
        }
    }
}
