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
                .body("visibility", equalTo("PUBLIC"))
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

        given().auth().oauth2(organizerToken).contentType(ContentType.JSON)
                .body(tournamentBody("Copa Privada").replace("\"rules\": \"Standard rules.\"", "\"rules\": \"Standard rules.\", \"visibility\": \"PRIVATE\""))
                .when().put("/api/v1/tournaments/{tournamentId}", tournamentId)
                .then().statusCode(200).body("visibility", equalTo("PRIVATE"));

        given().auth().oauth2(organizerToken)
                .when().post("/api/v1/tournaments/{tournamentId}/publish", tournamentId)
                .then().statusCode(200)
                .body("status", equalTo("OPEN"))
                .body("publishedAt", notNullValue());

        given().auth().oauth2(organizerToken).contentType(ContentType.JSON)
                .body(tournamentBody("Copa Publica").replace("\"rules\": \"Standard rules.\"", "\"rules\": \"Standard rules.\", \"visibility\": \"PUBLIC\""))
                .when().put("/api/v1/tournaments/{tournamentId}", tournamentId)
                .then().statusCode(409);

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

    @Test
    void authenticatedTeamAdminsDiscoverOnlyOpenPublicTournamentsAndCategories() {
        String organizerToken = accessToken("ORGANIZER");
        String athleteToken = accessToken("ATHLETE");
        String publicId = createTournament(organizerToken, "Copa Publica");
        String privateId = given().auth().oauth2(organizerToken).contentType(ContentType.JSON)
                .body(tournamentBody("Copa Privada").replace("\"rules\": \"Standard rules.\"", "\"rules\": \"Standard rules.\", \"visibility\": \"PRIVATE\""))
                .when().post("/api/v1/tournaments").then().statusCode(201).extract().jsonPath().getString("id");
        String categoryId = createCategory(organizerToken, publicId, "Aberta");
        given().auth().oauth2(organizerToken).when().post("/api/v1/tournaments/{id}/publish", publicId).then().statusCode(200);
        given().auth().oauth2(organizerToken).when().post("/api/v1/tournaments/{id}/publish", privateId).then().statusCode(200);

        given().auth().oauth2(athleteToken).when().get("/api/v1/tournaments/available?page=0&size=20")
                .then().statusCode(200).body("id", hasItem(publicId)).body("id", not(hasItem(privateId)));
        given().auth().oauth2(athleteToken).when().get("/api/v1/tournaments/{id}/overview", publicId)
                .then().statusCode(200).body("tournament.visibility", equalTo("PUBLIC"))
                .body("categories.id", hasItem(categoryId));
        given().auth().oauth2(athleteToken).when().get("/api/v1/tournaments/{id}/overview", privateId)
                .then().statusCode(404);
        given().auth().oauth2(athleteToken).when().get("/api/v1/tournaments/available?size=51")
                .then().statusCode(422);
    }

    @Test
    void managesTournamentCategoriesAndRealCategoryCounts() {
        String organizerToken = accessToken("ORGANIZER");
        String tournamentId = createTournament(organizerToken, "Copa Categorias");

        Response created = given().auth().oauth2(organizerToken)
                .contentType(ContentType.JSON)
                .body(categoryBody("Iniciante"))
                .when().post("/api/v1/tournaments/{tournamentId}/categories", tournamentId);

        String categoryId = created.then().statusCode(201)
                .header("Location", notNullValue())
                .body("id", notNullValue())
                .body("tournamentId", equalTo(tournamentId))
                .body("name", equalTo("Iniciante"))
                .body("modality", equalTo("FUTSAL"))
                .body("minAthletes", equalTo(7))
                .body("maxAthletes", equalTo(12))
                .body("minTeams", equalTo(4))
                .body("maxTeams", equalTo(16))
                .body("paymentModel", equalTo("TEAM"))
                .body("priceInCents", equalTo(12500))
                .body("competitionFormat", equalTo("GROUP_STAGE"))
                .body("swissRounds", equalTo(null))
                .body("matchDurationMinutes", equalTo(40))
                .body("intervalBetweenMatchesMinutes", equalTo(10))
                .body("minimumRestMinutes", equalTo(60))
                .body("schedulingStrategy", equalTo("BALANCED_REST"))
                .body("createdAt", notNullValue())
                .body("updatedAt", notNullValue())
                .extract().jsonPath().getString("id");

        given().auth().oauth2(organizerToken)
                .when().get("/api/v1/tournaments/{tournamentId}/categories", tournamentId)
                .then().statusCode(200)
                .body("size()", equalTo(1))
                .body("id", hasItem(categoryId));

        given().auth().oauth2(organizerToken)
                .when().get("/api/v1/tournaments/{tournamentId}", tournamentId)
                .then().statusCode(200)
                .body("categoryCount", equalTo(1));

        given().auth().oauth2(organizerToken)
                .contentType(ContentType.JSON)
                .body(categoryBody("Intermediario").replace("\"GROUP_STAGE\"", "\"SWISS\"")
                        .replace("\"swissRounds\": null", "\"swissRounds\": 5"))
                .when().put("/api/v1/tournaments/{tournamentId}/categories/{categoryId}", tournamentId, categoryId)
                .then().statusCode(200)
                .body("name", equalTo("Intermediario"))
                .body("competitionFormat", equalTo("SWISS"))
                .body("swissRounds", equalTo(5));

        given().auth().oauth2(organizerToken)
                .when().delete("/api/v1/tournaments/{tournamentId}/categories/{categoryId}", tournamentId, categoryId)
                .then().statusCode(204);

        given().auth().oauth2(organizerToken)
                .when().get("/api/v1/tournaments/{tournamentId}", tournamentId)
                .then().statusCode(200)
                .body("categoryCount", equalTo(0));
    }

    @Test
    void rejectsInvalidUnauthorizedNonOwnerDuplicateAndClosedCategoryOperations() {
        String organizerToken = accessToken("ORGANIZER");
        String otherToken = accessToken("ORGANIZER");
        String athleteToken = accessToken("ATHLETE");
        String tournamentId = createTournament(organizerToken, "Copa Categorias");
        String categoryId = createCategory(organizerToken, tournamentId, "Iniciante");

        given().contentType(ContentType.JSON).body(categoryBody("Sem Auth"))
                .when().post("/api/v1/tournaments/{tournamentId}/categories", tournamentId)
                .then().statusCode(401);

        given().auth().oauth2(athleteToken)
                .when().get("/api/v1/tournaments/{tournamentId}/categories", tournamentId)
                .then().statusCode(403);

        given().auth().oauth2(otherToken)
                .when().get("/api/v1/tournaments/{tournamentId}/categories", tournamentId)
                .then().statusCode(404)
                .body("code", equalTo("TOURNAMENT_NOT_FOUND"));

        given().auth().oauth2(organizerToken).contentType(ContentType.JSON).body(categoryBody("Iniciante"))
                .when().post("/api/v1/tournaments/{tournamentId}/categories", tournamentId)
                .then().statusCode(409)
                .body("code", equalTo("TOURNAMENT_CATEGORY_CONFLICT"));

        given().auth().oauth2(organizerToken).contentType(ContentType.JSON).body(categoryBody("AB"))
                .when().post("/api/v1/tournaments/{tournamentId}/categories", tournamentId)
                .then().statusCode(422);

        given().auth().oauth2(organizerToken).contentType(ContentType.JSON)
                .body(categoryBody("Extra").replace("\"schedulingStrategy\": \"BALANCED_REST\"", "\"schedulingStrategy\": \"BALANCED_REST\", \"unexpected\": true"))
                .when().post("/api/v1/tournaments/{tournamentId}/categories", tournamentId)
                .then().statusCode(422);

        given().auth().oauth2(organizerToken)
                .when().post("/api/v1/tournaments/{tournamentId}/publish", tournamentId)
                .then().statusCode(200);
        given().auth().oauth2(organizerToken)
                .when().post("/api/v1/tournaments/{tournamentId}/close", tournamentId)
                .then().statusCode(200);

        given().auth().oauth2(organizerToken).contentType(ContentType.JSON).body(categoryBody("Bloqueada"))
                .when().put("/api/v1/tournaments/{tournamentId}/categories/{categoryId}", tournamentId, categoryId)
                .then().statusCode(409)
                .body("code", equalTo("TOURNAMENT_STATE_CONFLICT"));
    }

    private static String createTournament(String token, String name) {
        return given().auth().oauth2(token).contentType(ContentType.JSON).body(tournamentBody(name))
                .when().post("/api/v1/tournaments")
                .then().statusCode(201)
                .extract().jsonPath().getString("id");
    }

    private static String createCategory(String token, String tournamentId, String name) {
        return given().auth().oauth2(token).contentType(ContentType.JSON).body(categoryBody(name))
                .when().post("/api/v1/tournaments/{tournamentId}/categories", tournamentId)
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

    private static String categoryBody(String name) {
        return """
                {
                  "name": "%s",
                  "modality": "FUTSAL",
                  "minAthletes": 7,
                  "maxAthletes": 12,
                  "minTeams": 4,
                  "maxTeams": 16,
                  "paymentModel": "TEAM",
                  "priceInCents": 12500,
                  "competitionFormat": "GROUP_STAGE",
                  "swissRounds": null,
                  "matchDurationMinutes": 40,
                  "intervalBetweenMatchesMinutes": 10,
                  "minimumRestMinutes": 60,
                  "schedulingStrategy": "BALANCED_REST"
                }
                """.formatted(name);
    }

    private static String uniqueEmail() {
        return "tournament-" + UUID.randomUUID() + "@example.test";
    }
}
