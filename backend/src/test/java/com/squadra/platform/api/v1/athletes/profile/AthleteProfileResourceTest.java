package com.squadra.platform.api.v1.athletes.profile;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
class AthleteProfileResourceTest {
    @Test
    void athleteCreatesAndEditsOwnSportsProfile() {
        String token = accessToken("ATHLETE");

        Response created = given().auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(profileBody("Marta", "FORWARD", "\"MIDFIELDER\"", "\"userId\":\"%s\",".formatted(UUID.randomUUID())))
                .when().put("/api/v1/athletes/me/profile");

        created.then().statusCode(200)
                .body("id", notNullValue())
                .body("displayName", equalTo("Marta"))
                .body("primaryPosition", equalTo("FORWARD"))
                .body("secondaryPositions[0]", equalTo("MIDFIELDER"))
                .body("dominantFoot", equalTo("LEFT"))
                .body("jerseyNumber", equalTo(10));

        String profileId = created.jsonPath().getString("id");

        Response edited = given().auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(profileBody("Rainha", "MIDFIELDER", "\"FORWARD\"", ""))
                .when().put("/api/v1/athletes/me/profile");

        edited.then().statusCode(200)
                .body("id", equalTo(profileId))
                .body("displayName", equalTo("Rainha"))
                .body("primaryPosition", equalTo("MIDFIELDER"))
                .body("secondaryPositions[0]", equalTo("FORWARD"));

        given().auth().oauth2(token)
                .when().get("/api/v1/athletes/me/profile")
                .then().statusCode(200)
                .body("id", equalTo(profileId))
                .body("displayName", equalTo("Rainha"));
    }

    @Test
    void returnsNotFoundBeforeProfileExists() {
        given().auth().oauth2(accessToken("ATHLETE"))
                .when().get("/api/v1/athletes/me/profile")
                .then().statusCode(404)
                .body("code", equalTo("PROFILE_NOT_FOUND"));
    }

    @Test
    void rejectsUnauthenticatedAndOrganizerProfileAccess() {
        given()
                .when().get("/api/v1/athletes/me/profile")
                .then().statusCode(401);

        String organizerToken = accessToken("ORGANIZER");

        given().auth().oauth2(organizerToken)
                .contentType(ContentType.JSON)
                .body(profileBody("Organizer", "FORWARD", "", ""))
                .when().put("/api/v1/athletes/me/profile")
                .then().statusCode(403);
    }

    @Test
    void rejectsInvalidProfilePayloads() {
        String token = accessToken("ATHLETE");

        given().auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(profileBody("M", "FORWARD", "", ""))
                .when().put("/api/v1/athletes/me/profile")
                .then().statusCode(422)
                .body("code", equalTo("VALIDATION_FAILED"));

        given().auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(profileBody("Marta", "STRIKER", "", ""))
                .when().put("/api/v1/athletes/me/profile")
                .then().statusCode(422)
                .body("code", equalTo("VALIDATION_FAILED"));

        given().auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(profileBody("Marta", "FORWARD", "\"MIDFIELDER\",\"MIDFIELDER\"", ""))
                .when().put("/api/v1/athletes/me/profile")
                .then().statusCode(422)
                .body("code", equalTo("VALIDATION_FAILED"));

        given().auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(profileBody("Marta", "FORWARD", "\"FORWARD\"", ""))
                .when().put("/api/v1/athletes/me/profile")
                .then().statusCode(422)
                .body("code", equalTo("VALIDATION_FAILED"));
    }

    private static String accessToken(String role) {
        String email = "profile-" + UUID.randomUUID() + "@example.test";
        register(email, role).then().statusCode(201);
        return login(email).jsonPath().getString("accessToken");
    }

    private static Response register(String email, String role) {
        return given()
                .contentType(ContentType.JSON)
                .body("{\"email\":\"%s\",\"password\":\"correct horse battery\",\"role\":\"%s\"}".formatted(email, role))
                .when().post("/api/v1/auth/register");
    }

    private static Response login(String email) {
        return given()
                .contentType(ContentType.JSON)
                .body("{\"email\":\"%s\",\"password\":\"correct horse battery\"}".formatted(email))
                .when().post("/api/v1/auth/login");
    }

    private static String profileBody(String displayName, String primaryPosition, String secondaryPositions, String prefix) {
        String positions = secondaryPositions == null || secondaryPositions.isBlank() ? "" : secondaryPositions;
        return """
                {
                  %s
                  "displayName": "%s",
                  "fullName": "Marta Vieira",
                  "birthDate": "1986-02-19",
                  "primaryPosition": "%s",
                  "secondaryPositions": [%s],
                  "dominantFoot": "LEFT",
                  "jerseyNumber": 10,
                  "city": "Dois Riachos",
                  "state": "AL",
                  "bio": "Football story"
                }
                """.formatted(prefix, displayName, primaryPosition, positions);
    }
}
