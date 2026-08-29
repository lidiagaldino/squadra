package com.squadra.platform.infra.security;
import com.squadra.platform.domain.entities.*;
import com.squadra.platform.domain.valueobjects.*;
import com.squadra.platform.domain.enums.*;
import com.squadra.platform.domain.repositories.*;
import com.squadra.platform.domain.services.*;
import com.squadra.platform.domain.exceptions.*;

import com.squadra.platform.domain.services.AccessTokenIssuer;
import com.squadra.platform.domain.entities.User;
import io.smallrye.config.ConfigMapping;
import jakarta.enterprise.context.ApplicationScoped;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@ApplicationScoped
public final class HmacJwtAccessTokenIssuer implements AccessTokenIssuer {
    private final JwtConfig config;
    public HmacJwtAccessTokenIssuer(JwtConfig config) { this.config = config; }
    @Override public String issue(User user) {
        Instant now = Instant.now();
        String header = encoded("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = encoded("{\"iss\":\"" + json(config.jwtIssuer()) + "\",\"sub\":\"" + user.id() + "\",\"upn\":\"" + json(user.email().value()) + "\",\"email\":\"" + json(user.email().value()) + "\",\"role\":\"" + user.role().name() + "\",\"groups\":[\"" + user.role().name() + "\"],\"iat\":" + now.getEpochSecond() + ",\"exp\":" + now.plusSeconds(config.accessTokenLifetimeSeconds()).getEpochSecond() + "}");
        String signed = header + "." + payload;
        return signed + "." + sign(signed);
    }
    private String sign(String value) {
        try { Mac mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(config.signingKey().getBytes(StandardCharsets.UTF_8), "HmacSHA256")); return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.US_ASCII))); }
        catch (Exception exception) { throw new IllegalStateException("Unable to sign access token", exception); }
    }
    private static String encoded(String value) { return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8)); }
    private static String json(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }
    @ConfigMapping(prefix = "squadra.identity")
    public interface JwtConfig { String signingKey(); String jwtIssuer(); long accessTokenLifetimeSeconds(); }
}
