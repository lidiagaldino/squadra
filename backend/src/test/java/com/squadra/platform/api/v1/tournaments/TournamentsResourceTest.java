package com.squadra.platform.api.v1.tournaments;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
class TournamentsResourceTest {
    @Test
    void organizerCreatesDraftTournamentWithoutCategories() {
        String organizerToken = accessToken("ORGANIZER");

        Response created = given().auth().oauth2(organizerToken)
                .contentType(ContentType.JSON)
                .body(tournamentBody("Copa Squadra"))
                .when().post("/api/v1/tournaments");

        created.then().statusCode(201)
                .header("Location", notNullValue())
                .body("id", notNullValue())
                .body("status", equalTo("DRAFT"))
                .body("name", equalTo("Copa Squadra"))
                .body("city", equalTo("Sao Paulo"))
                .body("venueName", equalTo("Arena Central"))
                .body("categoryCount", equalTo(0));
    }

    @Test
    void listsAndShowsOnlyOwnedTournaments() {
        String ownerToken = accessToken("ORGANIZER");
        String otherToken = accessToken("ORGANIZER");
        String ownedId = createTournament(ownerToken, "Copa Owner");
        String otherId = createTournament(otherToken, "Copa Other");

        given().auth().oauth2(ownerToken)
                .when().get("/api/v1/tournaments")
                .then().statusCode(200)
                .body("id", hasItem(ownedId))
                .body("id", not(hasItem(otherId)));

        given().auth().oauth2(otherToken)
                .when().get("/api/v1/tournaments/{tournamentId}", ownedId)
                .then().statusCode(404)
                .body("code", equalTo("TOURNAMENT_NOT_FOUND"));
    }

    @Test
    void updatesAndTransitionsTournamentLifecycle() {
        String organizerToken = accessToken("ORGANIZER");
        String tournamentId = createTournament(organizerToken, "Copa Squadra");

        given().auth().oauth2(organizerToken)
                .contentType(ContentType.JSON)
                .body(tournamentBody("Copa Atualizada"))
                .when().put("/api/v1/tournaments/{tournamentId}", tournamentId)
                .then().statusCode(200)
                .body("name", equalTo("Copa Atualizada"));

        given().auth().oauth2(organizerToken)
                .when().post("/api/v1/tournaments/{tournamentId}/publish", tournamentId)
                .then().statusCode(200)
                .body("status", equalTo("OPEN"))
                .body("publishedAt", notNullValue());

        given().auth().oauth2(organizerToken)
                .when().post("/api/v1/tournaments/{tournamentId}/close", tournamentId)
                .then().statusCode(200)
                .body("status", equalTo("CLOSED"))
                .body("closedAt", notNullValue());

        given().auth().oauth2(organizerToken)
                .when().post("/api/v1/tournaments/{tournamentId}/reopen", tournamentId)
                .then().statusCode(200)
                .body("status", equalTo("OPEN"));

        given().auth().oauth2(organizerToken)
                .when().post("/api/v1/tournaments/{tournamentId}/cancel", tournamentId)
                .then().statusCode(200)
                .body("status", equalTo("CANCELLED"))
                .body("cancelledAt", notNullValue());
    }

    @Test
    void rejectsUnauthenticatedAthleteInvalidUnknownCategoryAndConflictRequests() {
        String organizerToken = accessToken("ORGANIZER");
        String athleteToken = accessToken("ATHLETE");
        String tournamentId = createTournament(organizerToken, "Copa Squadra");

        given().contentType(ContentType.JSON).body(tournamentBody("No Auth"))
                .when().post("/api/v1/tournaments")
                .then().statusCode(401);

        given().auth().oauth2(athleteToken).contentType(ContentType.JSON).body(tournamentBody("Athlete Event"))
                .when().post("/api/v1/tournaments")
                .then().statusCode(403);

        given().auth().oauth2(organizerToken).contentType(ContentType.JSON).body(tournamentBody("Co"))
                .when().post("/api/v1/tournaments")
                .then().statusCode(422);

        given().auth().oauth2(organizerToken).contentType(ContentType.JSON)
                .body(tournamentBody("Copa Extra").replace("\"rules\": \"Standard rules.\"", "\"rules\": \"Standard rules.\", \"unexpected\": true"))
                .when().post("/api/v1/tournaments")
                .then().statusCode(422);

        given().auth().oauth2(organizerToken).contentType(ContentType.JSON)
                .body(tournamentBody("Copa Categorias").replace("\"rules\": \"Standard rules.\"", "\"rules\": \"Standard rules.\", \"categories\": []"))
                .when().post("/api/v1/tournaments")
                .then().statusCode(422);

        given().auth().oauth2(organizerToken)
                .when().post("/api/v1/tournaments/{tournamentId}/close", tournamentId)
                .then().statusCode(409)
                .body("code", equalTo("TOURNAMENT_STATE_CONFLICT"));
    }

    @Test
    void cancelledTournamentRemainsVisibleButReadOnly() {
        String organizerToken = accessToken("ORGANIZER");
        String tournamentId = createTournament(organizerToken, "Copa Squadra");
        given().auth().oauth2(organizerToken)
                .when().post("/api/v1/tournaments/{tournamentId}/cancel", tournamentId)
                .then().statusCode(200);

        given().auth().oauth2(organizerToken)
                .when().get("/api/v1/tournaments/{tournamentId}", tournamentId)
                .then().statusCode(200)
                .body("status", equalTo("CANCELLED"));

        given().auth().oauth2(organizerToken).contentType(ContentType.JSON).body(tournamentBody("Copa Editada"))
                .when().put("/api/v1/tournaments/{tournamentId}", tournamentId)
                .then().statusCode(409);
    }

    @Test
    void returnsEmptyListWhenOrganizerHasNoTournaments() {
        given().auth().oauth2(accessToken("ORGANIZER"))
                .when().get("/api/v1/tournaments")
                .then().statusCode(200)
                .body("size()", equalTo(0));
    }

    private static String createTournament(String token, String name) {
        return given().auth().oauth2(token).contentType(ContentType.JSON).body(tournamentBody(name))
                .when().post("/api/v1/tournaments")
                .then().statusCode(201)
                .extract().jsonPath().getString("id");
    }

    private static String accessToken(String role) {
        String email = uniqueEmail();
        register(email, role).then().statusCode(201);
        return login(email).jsonPath().getString("accessToken");
    }

    private static Response register(String email, String role) {
        return given().contentType(ContentType.JSON)
                .body("{\"email\":\"%s\",\"password\":\"correct horse battery\",\"role\":\"%s\"}".formatted(email, role))
                .when().post("/api/v1/auth/register");
    }

    private static Response login(String email) {
        return given().contentType(ContentType.JSON)
                .body("{\"email\":\"%s\",\"password\":\"correct horse battery\"}".formatted(email))
                .when().post("/api/v1/auth/login");
    }

    private static String tournamentBody(String name) {
        return """
                {
                  "name": "%s",
                  "description": "Women's football tournament.",
                  "city": "Sao Paulo",
                  "state": "SP",
                  "venueName": "Arena Central",
                  "startDate": "2026-10-10",
                  "endDate": "2026-10-12",
                  "registrationDeadline": "2026-10-01",
                  "rules": "Standard rules."
                }
                """.formatted(name);
    }

    private static String uniqueEmail() {
        return "tournament-" + UUID.randomUUID() + "@example.test";
    }
}
