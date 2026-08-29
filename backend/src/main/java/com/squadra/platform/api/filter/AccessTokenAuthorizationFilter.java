package com.squadra.platform.api.filter;
import com.squadra.platform.domain.entities.*;
import com.squadra.platform.domain.valueobjects.*;
import com.squadra.platform.domain.enums.*;
import com.squadra.platform.domain.repositories.*;
import com.squadra.platform.domain.services.*;
import com.squadra.platform.domain.exceptions.*;

import com.squadra.platform.domain.services.AccessTokenVerifier;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;

@Provider @RequiresRoles({}) @Priority(Priorities.AUTHENTICATION)
public final class AccessTokenAuthorizationFilter implements ContainerRequestFilter {
    @Inject AccessTokenVerifier verifier;
    @Inject ResourceInfo resourceInfo;
    @Override public void filter(ContainerRequestContext request) throws IOException {
        String header = request.getHeaderString("Authorization");
        if (header == null || !header.startsWith("Bearer ")) { deny(request, 401); return; }
        try {
            var token = verifier.verify(header.substring(7));
            RequiresRoles roles = resourceInfo.getResourceMethod().getAnnotation(RequiresRoles.class);
            if (roles == null || Arrays.stream(roles.value()).noneMatch(role -> role == token.role())) { deny(request, 403); return; }
            request.setProperty("access-token", token);
        } catch (IllegalArgumentException exception) { deny(request, 401); }
    }
    private static void deny(ContainerRequestContext request, int status) { request.abortWith(Response.status(status).header("WWW-Authenticate", "Bearer").build()); }
}
