package com.squadra.platform.api.v1.athletes.profile.mapper;

import com.squadra.platform.api.v1.athletes.profile.response.AthleteProfileResponses;
import com.squadra.platform.domain.exceptions.AthleteProfileNotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public final class AthleteProfileExceptionMapper implements ExceptionMapper<AthleteProfileNotFoundException> {
    @Override
    public Response toResponse(AthleteProfileNotFoundException exception) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(new AthleteProfileResponses.Error("PROFILE_NOT_FOUND", "Athlete profile was not found"))
                .build();
    }
}
