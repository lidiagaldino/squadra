package com.squadra.platform.api.v1.organizers.profile;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
class OrganizerProfileResourceTest {
    @Test
    void organizerCreatesAndEditsOwnOrganizationProfile() {
        String token = accessToken("ORGANIZER");

        Response created = given().auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(profileBody("Liga Miga", "https://ligamiga.test", "\"userId\":\"%s\",".formatted(UUID.randomUUID())))
                .when().put("/api/v1/organizers/me/profile");

        created.then().statusCode(200)
                .body("id", notNullValue())
                .body("organizationName", equalTo("Liga Miga"))
                .body("contactName", equalTo("Ana Silva"))
                .body("contactEmail", equalTo("contato@ligamiga.test"))
                .body("phone", equalTo("+55 11 99999-0000"))
                .body("city", equalTo("Sao Paulo"))
                .body("state", equalTo("SP"))
                .body("website", equalTo("https://ligamiga.test"))
                .body("description", equalTo("Organizes women's football tournaments."))
                .body("createdAt", notNullValue())
                .body("updatedAt", notNullValue());

        String profileId = created.jsonPath().getString("id");

        Response edited = given().auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(profileBody("Copa Minas", "http://copaminas.test", ""))
                .when().put("/api/v1/organizers/me/profile");

        edited.then().statusCode(200)
                .body("id", equalTo(profileId))
                .body("organizationName", equalTo("Copa Minas"))
                .body("website", equalTo("http://copaminas.test"));

        given().auth().oauth2(token)
                .when().get("/api/v1/organizers/me/profile")
                .then().statusCode(200)
                .body("id", equalTo(profileId))
                .body("organizationName", equalTo("Copa Minas"));
    }

    @Test
    void repeatedSameSubmissionKeepsOneProfileRowForOrganizer() {
        String token = accessToken("ORGANIZER");

        String firstProfileId = given().auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(profileBody("Liga Miga", "https://ligamiga.test", ""))
                .when().put("/api/v1/organizers/me/profile")
                .then().statusCode(200)
                .extract().jsonPath().getString("id");

        given().auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(profileBody("Liga Miga", "https://ligamiga.test", ""))
                .when().put("/api/v1/organizers/me/profile")
                .then().statusCode(200)
                .body("id", equalTo(firstProfileId))
                .body("organizationName", equalTo("Liga Miga"));
    }

    @Test
    void acceptsMinimalProfileWithBlankOptionalFields() {
        given().auth().oauth2(accessToken("ORGANIZER"))
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "organizationName": "Liga Miga",
                          "contactName": "Ana Silva",
                          "contactEmail": "contato@ligamiga.test",
                          "phone": "",
                          "city": "Sao Paulo",
                          "state": "SP",
                          "website": "",
                          "description": ""
                        }
                        """)
                .when().put("/api/v1/organizers/me/profile")
                .then().statusCode(200)
                .body("organizationName", equalTo("Liga Miga"))
                .body("phone", equalTo(null))
                .body("website", equalTo(null))
                .body("description", equalTo(null));
    }

    @Test
    void returnsNotFoundBeforeProfileExists() {
        given().auth().oauth2(accessToken("ORGANIZER"))
                .when().get("/api/v1/organizers/me/profile")
                .then().statusCode(404)
                .body("code", equalTo("ORGANIZER_PROFILE_NOT_FOUND"));
    }

    @Test
    void rejectsUnauthenticatedAndAthleteProfileAccess() {
        given()
                .when().get("/api/v1/organizers/me/profile")
                .then().statusCode(401);

        given()
                .contentType(ContentType.JSON)
                .body(profileBody("Liga Miga", "https://ligamiga.test", ""))
                .when().put("/api/v1/organizers/me/profile")
                .then().statusCode(401);

        String athleteToken = accessToken("ATHLETE");

        given().auth().oauth2(athleteToken)
                .contentType(ContentType.JSON)
                .body(profileBody("Liga Miga", "https://ligamiga.test", ""))
                .when().put("/api/v1/organizers/me/profile")
                .then().statusCode(403);
    }

    @Test
    void rejectsInvalidProfilePayloads() {
        String token = accessToken("ORGANIZER");

        given().auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(profileBody("L", "https://ligamiga.test", ""))
                .when().put("/api/v1/organizers/me/profile")
                .then().statusCode(422)
                .body("code", equalTo("VALIDATION_FAILED"));

        given().auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(profileBody("Liga Miga", "ftp://ligamiga.test", ""))
                .when().put("/api/v1/organizers/me/profile")
                .then().statusCode(422)
                .body("code", equalTo("VALIDATION_FAILED"));

        given().auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "organizationName": "Liga Miga",
                          "contactName": "Ana Silva",
                          "contactEmail": "not-an-email",
                          "city": "Sao Paulo",
                          "state": "SP"
                        }
                        """)
                .when().put("/api/v1/organizers/me/profile")
                .then().statusCode(422)
                .body("code", equalTo("VALIDATION_FAILED"));

        given().auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "organizationName": "Liga Miga",
                          "contactName": "Ana Silva",
                          "contactEmail": "contato@ligamiga.test",
                          "phone": "%s",
                          "city": "Sao Paulo",
                          "state": "SP"
                        }
                        """.formatted("1".repeat(31)))
                .when().put("/api/v1/organizers/me/profile")
                .then().statusCode(422)
                .body("code", equalTo("VALIDATION_FAILED"));
    }

    private static String accessToken(String role) {
        String email = "organizer-profile-" + UUID.randomUUID() + "@example.test";
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

    private static String profileBody(String organizationName, String website, String prefix) {
        return """
                {
                  %s
                  "organizationName": "%s",
                  "contactName": "Ana Silva",
                  "contactEmail": "contato@ligamiga.test",
                  "phone": "+55 11 99999-0000",
                  "city": "Sao Paulo",
                  "state": "SP",
                  "website": "%s",
                  "description": "Organizes women's football tournaments."
                }
                """.formatted(prefix, organizationName, website);
    }
}
