package com.squadra.platform.api.v1.organizers.profile.mapper;

import com.squadra.platform.api.v1.organizers.profile.response.OrganizerProfileResponses;
import com.squadra.platform.domain.exceptions.OrganizerProfileNotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public final class OrganizerProfileExceptionMapper implements ExceptionMapper<OrganizerProfileNotFoundException> {
    @Override
    public Response toResponse(OrganizerProfileNotFoundException exception) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(new OrganizerProfileResponses.Error("ORGANIZER_PROFILE_NOT_FOUND",
                        "Organizer profile was not found"))
                .build();
    }
}
