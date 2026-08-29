package com.squadra.platform.infra.security;
import com.squadra.platform.domain.entities.*;
import com.squadra.platform.domain.valueobjects.*;
import com.squadra.platform.domain.enums.*;
import com.squadra.platform.domain.repositories.*;
import com.squadra.platform.domain.services.*;
import com.squadra.platform.domain.exceptions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squadra.platform.domain.services.AccessTokenVerifier;
import com.squadra.platform.domain.valueobjects.EmailAddress;
import com.squadra.platform.domain.enums.UserRole;
import io.smallrye.config.ConfigMapping;
import jakarta.enterprise.context.ApplicationScoped;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@ApplicationScoped
public final class HmacJwtAccessTokenVerifier implements AccessTokenVerifier {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final JwtConfig config;
    public HmacJwtAccessTokenVerifier(JwtConfig config) { this.config = config; }

    @Override public VerifiedAccessToken verify(String rawToken) {
        try {
            String[] parts = rawToken.split("\\.", -1);
            if (parts.length != 3 || !MessageDigest.isEqual(signature(parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII), parts[2].getBytes(StandardCharsets.US_ASCII))) throw invalid();
            JsonNode header = JSON.readTree(decoded(parts[0]));
            JsonNode claims = JSON.readTree(decoded(parts[1]));
            if (!"HS256".equals(header.path("alg").asText()) || !config.jwtIssuer().equals(claims.path("iss").asText()) || claims.path("exp").asLong(0) <= Instant.now().getEpochSecond()) throw invalid();
            UserRole role = UserRole.valueOf(claims.path("role").asText());
            if (!claims.path("groups").isArray() || !containsRole(claims.path("groups"), role)) throw invalid();
            return new VerifiedAccessToken(UUID.fromString(claims.path("sub").asText()), new EmailAddress(claims.path("email").asText()), role);
        } catch (Exception exception) { throw invalid(); }
    }
    private boolean containsRole(JsonNode groups, UserRole role) { for (JsonNode group : groups) if (role.name().equals(group.asText())) return true; return false; }
    private String signature(String input) throws Exception { Mac mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(config.signingKey().getBytes(StandardCharsets.UTF_8), "HmacSHA256")); return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(input.getBytes(StandardCharsets.US_ASCII))); }
    private static String decoded(String value) { return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8); }
    private static IllegalArgumentException invalid() { return new IllegalArgumentException("Invalid access token"); }
    @ConfigMapping(prefix = "squadra.identity") public interface JwtConfig { String signingKey(); String jwtIssuer(); }
}
