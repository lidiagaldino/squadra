package com.squadra.platform.api.v1.auth;
import com.squadra.platform.domain.entities.*;
import com.squadra.platform.domain.valueobjects.*;
import com.squadra.platform.domain.enums.*;
import com.squadra.platform.domain.repositories.*;
import com.squadra.platform.domain.services.*;
import com.squadra.platform.domain.exceptions.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.PreparedStatement;
import java.time.Instant;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

@QuarkusTest
class AuthenticationResourceTest {
    private static final String REFRESH_COOKIE = "squadra_refresh_token";

    @Inject
    DataSource dataSource;

    @Test
    void registers_an_athlete_without_returning_credentials() {
        String email = uniqueEmail();

        given()
                .contentType(ContentType.JSON)
                .body("{\"email\":\"%s\",\"password\":\"correct horse battery\",\"role\":\"ATHLETE\"}".formatted(email))
                .when().post("/api/v1/auth/register")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("email", equalTo(email))
                .body("role", equalTo("ATHLETE"));
    }

    @Test
    void creates_rotates_and_revokes_the_http_only_refresh_session() {
        String email = uniqueEmail();
        register(email, "ORGANIZER");

        Response login = login(email);
        login.then().statusCode(200).body("accessToken", notNullValue());
        assertSecureRefreshCookie(login);
        String firstToken = login.getCookie(REFRESH_COOKIE);

        Response refresh = given().cookie(REFRESH_COOKIE, firstToken)
                .when().post("/api/v1/auth/refresh");
        refresh.then().statusCode(200).body("accessToken", notNullValue());
        String replacementToken = refresh.getCookie(REFRESH_COOKIE);
        assertNotEquals(firstToken, replacementToken);

        given().cookie(REFRESH_COOKIE, firstToken)
                .when().post("/api/v1/auth/refresh")
                .then().statusCode(401)
                .body("code", equalTo("AUTHENTICATION_FAILED"));

        Response logout = given().cookie(REFRESH_COOKIE, replacementToken)
                .when().post("/api/v1/auth/logout");
        logout.then().statusCode(204);
        assertTrue(logout.getHeader("Set-Cookie").contains("Max-Age=0"));

        given().cookie(REFRESH_COOKIE, replacementToken)
                .when().post("/api/v1/auth/refresh")
                .then().statusCode(401)
                .body("code", equalTo("AUTHENTICATION_FAILED"));
    }

    @Test
    void logout_clears_the_refresh_cookie_without_an_active_session() {
        Response logout = given()
                .when().post("/api/v1/auth/logout");

        logout.then().statusCode(204);
        assertTrue(logout.getHeader("Set-Cookie").contains("Max-Age=0"));
    }

    @Test
    void rejects_invalid_credentials_and_does_not_enumerate_unknown_recovery_addresses() {
        Response login = login(uniqueEmail());
        login.then().statusCode(401)
                .body("code", equalTo("AUTHENTICATION_FAILED"));

        given()
                .contentType(ContentType.JSON)
                .body("{\"email\":\"%s\"}".formatted(uniqueEmail()))
                .when().post("/api/v1/auth/password-recovery")
                .then().statusCode(202);
    }

    @Test
    void validates_access_tokens_and_roles_before_returning_the_session() {
        String email = uniqueEmail();
        Response registered = register(email, "ORGANIZER");
        String accessToken = login(email).jsonPath().getString("accessToken");
        given().header("Authorization", "Bearer " + accessToken).when().get("/api/v1/auth/session")
                .then().statusCode(200).body("id", equalTo(registered.jsonPath().getString("id"))).body("role", equalTo("ORGANIZER"));
        given().header("Authorization", "Bearer " + accessToken.substring(0, accessToken.length() - 1) + "x").when().get("/api/v1/auth/session")
                .then().statusCode(401);
    }

    @Test
    void maps_duplicate_accounts_and_malformed_registration_to_safe_client_errors() {
        String email = uniqueEmail();
        register(email, "ATHLETE").then().statusCode(201);

        register(email, "ATHLETE").then().statusCode(409)
                .body("code", equalTo("ACCOUNT_ALREADY_EXISTS"));
        given()
                .contentType(ContentType.JSON)
                .body("{\"email\":\"not-an-email\",\"password\":\"short\",\"role\":\"ADMIN\"}")
                .when().post("/api/v1/auth/register")
                .then().statusCode(422)
                .body("code", equalTo("VALIDATION_FAILED"));
    }

    @Test
    void resets_the_password_and_revokes_existing_refresh_sessions() throws Exception {
        String email = uniqueEmail();
        Response registration = register(email, "ATHLETE");
        Response session = login(email);
        String refreshToken = session.getCookie(REFRESH_COOKIE);
        String resetToken = "reset-" + UUID.randomUUID();
        insertResetToken(UUID.fromString(registration.jsonPath().getString("id")), resetToken);

        given()
                .contentType(ContentType.JSON)
                .body("{\"token\":\"%s\",\"password\":\"new correct horse battery\"}".formatted(resetToken))
                .when().post("/api/v1/auth/password-reset")
                .then().statusCode(204);

        given().cookie(REFRESH_COOKIE, refreshToken)
                .when().post("/api/v1/auth/refresh")
                .then().statusCode(401);
        login(email).then().statusCode(401);
        login(email, "new correct horse battery").then().statusCode(200);
    }

    private static Response register(String email, String role) {
        return given()
                .contentType(ContentType.JSON)
                .body("{\"email\":\"%s\",\"password\":\"correct horse battery\",\"role\":\"%s\"}".formatted(email, role))
                .when().post("/api/v1/auth/register");
    }

    private static Response login(String email) {
        return login(email, "correct horse battery");
    }

    private static Response login(String email, String password) {
        return given()
                .contentType(ContentType.JSON)
                .body("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password))
                .when().post("/api/v1/auth/login");
    }

    private void insertResetToken(UUID userId, String rawToken) throws Exception {
        String sql = "INSERT INTO squadra.password_reset_tokens (id, user_id, token_hash, expires_at, used_at, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        Instant now = Instant.now();
        try (var connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, UUID.randomUUID());
            statement.setObject(2, userId);
            statement.setString(3, sha256(rawToken));
            statement.setObject(4, now.plusSeconds(1_800));
            statement.setObject(5, null);
            statement.setObject(6, now);
            statement.executeUpdate();
        }
    }

    private static String sha256(String value) throws Exception {
        return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    private static void assertSecureRefreshCookie(Response response) {
        String setCookie = response.getHeader("Set-Cookie");
        assertTrue(setCookie.contains("HttpOnly"));
        assertTrue(setCookie.contains("Secure"));
        assertTrue(setCookie.contains("SameSite=Strict"));
        assertTrue(setCookie.contains("Path=/api/v1/auth"));
    }

    private static String uniqueEmail() {
        return "auth-" + UUID.randomUUID() + "@example.test";
    }
}
