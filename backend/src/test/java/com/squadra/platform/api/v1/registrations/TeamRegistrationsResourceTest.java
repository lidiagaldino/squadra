package com.squadra.platform.api.v1.registrations;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

@QuarkusTest
class TeamRegistrationsResourceTest {
    @Test
    void teamAdminRegistersInOpenTournamentCategoryAndOrganizerListsIt() {
        String ownerToken = accessToken("ATHLETE");
        String organizerToken = accessToken("ORGANIZER");
        String teamId = createTeam(ownerToken, "Estrelas FC");
        String tournamentId = createOpenTournament(organizerToken, "Copa Registro");
        String categoryId = createCategory(organizerToken, tournamentId, "Iniciante", 16);

        Response created = given().auth().oauth2(ownerToken)
                .contentType(ContentType.JSON)
                .body(registerBody(teamId))
                .when().post("/api/v1/tournaments/{tournamentId}/categories/{categoryId}/registrations", tournamentId, categoryId);

        String registrationId = created.then().statusCode(201)
                .header("Location", notNullValue())
                .body("id", notNullValue())
                .body("teamId", equalTo(teamId))
                .body("teamName", equalTo("Estrelas FC"))
                .body("tournamentId", equalTo(tournamentId))
                .body("tournamentName", equalTo("Copa Registro"))
                .body("categoryId", equalTo(categoryId))
                .body("categoryName", equalTo("Iniciante"))
                .body("status", equalTo("DRAFT"))
                .body("createdAt", notNullValue())
                .body("updatedAt", notNullValue())
                .extract().jsonPath().getString("id");

        given().auth().oauth2(organizerToken)
                .when().get("/api/v1/tournaments/{tournamentId}/categories/{categoryId}/registrations", tournamentId, categoryId)
                .then().statusCode(200)
                .body("id", hasItem(registrationId))
                .body("teamName", hasItem("Estrelas FC"))
                .body("status", hasItem("DRAFT"));

        given().auth().oauth2(ownerToken)
                .when().get("/api/v1/teams/{teamId}/registrations", teamId)
                .then().statusCode(200)
                .body("id", hasItem(registrationId))
                .body("tournamentName", hasItem("Copa Registro"))
                .body("categoryName", hasItem("Iniciante"));
    }

    @Test
    void teamAdminCancelsDraftRegistrationAndCanRegisterAgain() {
        String ownerToken = accessToken("ATHLETE");
        String organizerToken = accessToken("ORGANIZER");
        String teamId = createTeam(ownerToken, "Lobas FC");
        String tournamentId = createOpenTournament(organizerToken, "Copa Cancelamento");
        String categoryId = createCategory(organizerToken, tournamentId, "Intermediario", 16);
        String registrationId = createRegistration(ownerToken, tournamentId, categoryId, teamId);

        given().auth().oauth2(ownerToken)
                .when().post("/api/v1/teams/{teamId}/registrations/{registrationId}/cancel", teamId, registrationId)
                .then().statusCode(200)
                .body("id", equalTo(registrationId))
                .body("status", equalTo("CANCELLED"));

        given().auth().oauth2(ownerToken)
                .contentType(ContentType.JSON)
                .body(registerBody(teamId))
                .when().post("/api/v1/tournaments/{tournamentId}/categories/{categoryId}/registrations", tournamentId, categoryId)
                .then().statusCode(201)
                .body("status", equalTo("DRAFT"));
    }

    @Test
    void rejectsDuplicateAndFullCategoryRegistrations() {
        String organizerToken = accessToken("ORGANIZER");
        String ownerOneToken = accessToken("ATHLETE");
        String ownerTwoToken = accessToken("ATHLETE");
        String ownerThreeToken = accessToken("ATHLETE");
        String teamOne = createTeam(ownerOneToken, "Primeiras FC");
        String teamTwo = createTeam(ownerTwoToken, "Segundas FC");
        String teamThree = createTeam(ownerThreeToken, "Terceiras FC");
        String tournamentId = createOpenTournament(organizerToken, "Copa Limite");
        String categoryId = createCategory(organizerToken, tournamentId, "Elite", 2);

        createRegistration(ownerOneToken, tournamentId, categoryId, teamOne);

        given().auth().oauth2(ownerOneToken)
                .contentType(ContentType.JSON)
                .body(registerBody(teamOne))
                .when().post("/api/v1/tournaments/{tournamentId}/categories/{categoryId}/registrations", tournamentId, categoryId)
                .then().statusCode(409)
                .body("code", equalTo("TEAM_REGISTRATION_CONFLICT"));

        createRegistration(ownerTwoToken, tournamentId, categoryId, teamTwo);

        given().auth().oauth2(ownerThreeToken)
                .contentType(ContentType.JSON)
                .body(registerBody(teamThree))
                .when().post("/api/v1/tournaments/{tournamentId}/categories/{categoryId}/registrations", tournamentId, categoryId)
                .then().statusCode(409)
                .body("code", equalTo("TEAM_REGISTRATION_CONFLICT"));
    }

    @Test
    void concurrentRegistrationsCannotExceedCategoryCapacity() {
        String organizerToken = accessToken("ORGANIZER");
        String firstToken = accessToken("ATHLETE");
        String secondToken = accessToken("ATHLETE");
        String thirdToken = accessToken("ATHLETE");
        String firstTeam = createTeam(firstToken, "Primeiro Time");
        String secondTeam = createTeam(secondToken, "Segundo Time");
        String thirdTeam = createTeam(thirdToken, "Terceiro Time");
        String tournamentId = createOpenTournament(organizerToken, "Copa Concorrente");
        String categoryId = createCategory(organizerToken, tournamentId, "Unica", 2);
        createRegistration(firstToken, tournamentId, categoryId, firstTeam);

        CompletableFuture<Integer> second = CompletableFuture.supplyAsync(
                () -> registrationStatus(secondToken, tournamentId, categoryId, secondTeam));
        CompletableFuture<Integer> third = CompletableFuture.supplyAsync(
                () -> registrationStatus(thirdToken, tournamentId, categoryId, thirdTeam));

        int secondStatus = second.join();
        int thirdStatus = third.join();
        assertEquals(1, (secondStatus == 201 ? 1 : 0) + (thirdStatus == 201 ? 1 : 0));
        assertEquals(1, (secondStatus == 409 ? 1 : 0) + (thirdStatus == 409 ? 1 : 0));
        given().auth().oauth2(organizerToken)
                .when().get("/api/v1/tournaments/{tournamentId}/categories/{categoryId}/registrations", tournamentId, categoryId)
                .then().statusCode(200).body("size()", equalTo(2));
    }

    @Test
    void rejectsInvalidStatusContextAndUnauthorizedRegistrationAccess() {
        String organizerToken = accessToken("ORGANIZER");
        String otherOrganizerToken = accessToken("ORGANIZER");
        String ownerToken = accessToken("ATHLETE");
        String athleteToken = accessToken("ATHLETE");
        String teamId = createTeam(ownerToken, "Contexto FC");
        String tournamentId = createTournament(organizerToken, "Copa Contexto");
        String categoryId = createCategory(organizerToken, tournamentId, "Aberta", 16);

        given().contentType(ContentType.JSON)
                .body(registerBody(teamId))
                .when().post("/api/v1/tournaments/{tournamentId}/categories/{categoryId}/registrations", tournamentId, categoryId)
                .then().statusCode(401);

        given().auth().oauth2(ownerToken).contentType(ContentType.JSON)
                .body(registerBody(teamId))
                .when().post("/api/v1/tournaments/{tournamentId}/categories/{categoryId}/registrations", tournamentId, categoryId)
                .then().statusCode(409)
                .body("code", equalTo("TOURNAMENT_STATE_CONFLICT"));

        given().auth().oauth2(athleteToken).contentType(ContentType.JSON)
                .body(registerBody(teamId))
                .when().post("/api/v1/tournaments/{tournamentId}/categories/{categoryId}/registrations", tournamentId, categoryId)
                .then().statusCode(403)
                .body("code", equalTo("TEAM_FORBIDDEN"));

        given().auth().oauth2(ownerToken)
                .when().get("/api/v1/teams/{teamId}/registrations", teamId)
                .then().statusCode(200)
                .body("size()", equalTo(0));

        given().auth().oauth2(otherOrganizerToken)
                .when().get("/api/v1/tournaments/{tournamentId}/categories/{categoryId}/registrations", tournamentId, categoryId)
                .then().statusCode(404)
                .body("code", equalTo("TOURNAMENT_NOT_FOUND"));

        given().auth().oauth2(ownerToken).contentType(ContentType.JSON)
                .body(registerBody(teamId))
                .when().post("/api/v1/tournaments/{tournamentId}/categories/{categoryId}/registrations", tournamentId, UUID.randomUUID())
                .then().statusCode(404)
                .body("code", equalTo("TOURNAMENT_CATEGORY_NOT_FOUND"));

        given().auth().oauth2(ownerToken).contentType(ContentType.JSON)
                .body(registerBody(teamId).replace("}", ", \"unexpected\": true}"))
                .when().post("/api/v1/tournaments/{tournamentId}/categories/{categoryId}/registrations", tournamentId, categoryId)
                .then().statusCode(422);
    }

    @Test
    void referencedCategoryCannotBeDeleted() {
        String organizerToken = accessToken("ORGANIZER");
        String ownerToken = accessToken("ATHLETE");
        String teamId = createTeam(ownerToken, "Historico FC");
        String tournamentId = createOpenTournament(organizerToken, "Copa Historico");
        String categoryId = createCategory(organizerToken, tournamentId, "Principal", 16);
        createRegistration(ownerToken, tournamentId, categoryId, teamId);

        given().auth().oauth2(organizerToken)
                .when().delete("/api/v1/tournaments/{tournamentId}/categories/{categoryId}", tournamentId, categoryId)
                .then().statusCode(409)
                .body("code", equalTo("TOURNAMENT_CATEGORY_CONFLICT"));
    }

    private static String createRegistration(String token, String tournamentId, String categoryId, String teamId) {
        return given().auth().oauth2(token).contentType(ContentType.JSON).body(registerBody(teamId))
                .when().post("/api/v1/tournaments/{tournamentId}/categories/{categoryId}/registrations", tournamentId, categoryId)
                .then().statusCode(201)
                .extract().jsonPath().getString("id");
    }

    private static int registrationStatus(String token, String tournamentId, String categoryId, String teamId) {
        return given().auth().oauth2(token).contentType(ContentType.JSON).body(registerBody(teamId))
                .when().post("/api/v1/tournaments/{tournamentId}/categories/{categoryId}/registrations", tournamentId, categoryId)
                .statusCode();
    }

    private static String createOpenTournament(String token, String name) {
        String tournamentId = createTournament(token, name);
        given().auth().oauth2(token)
                .when().post("/api/v1/tournaments/{tournamentId}/publish", tournamentId)
                .then().statusCode(200);
        return tournamentId;
    }

    private static String createTournament(String token, String name) {
        return given().auth().oauth2(token).contentType(ContentType.JSON).body(tournamentBody(name))
                .when().post("/api/v1/tournaments")
                .then().statusCode(201)
                .extract().jsonPath().getString("id");
    }

    private static String createCategory(String token, String tournamentId, String name, int maxTeams) {
        return given().auth().oauth2(token).contentType(ContentType.JSON).body(categoryBody(name, maxTeams))
                .when().post("/api/v1/tournaments/{tournamentId}/categories", tournamentId)
                .then().statusCode(201)
                .extract().jsonPath().getString("id");
    }

    private static String createTeam(String token, String name) {
        return given().auth().oauth2(token).contentType(ContentType.JSON).body(teamBody(name))
                .when().post("/api/v1/teams")
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

    private static String registerBody(String teamId) {
        return "{\"teamId\":\"%s\"}".formatted(teamId);
    }

    private static String teamBody(String name) {
        return """
                {
                  "name": "%s",
                  "city": "Sao Paulo",
                  "state": "SP",
                  "foundedYear": 2020,
                  "description": "Women's football team."
                }
                """.formatted(name);
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

    private static String categoryBody(String name, int maxTeams) {
        return """
                {
                  "name": "%s",
                  "modality": "FUTSAL",
                  "minAthletes": 7,
                  "maxAthletes": 12,
                  "minTeams": 2,
                  "maxTeams": %d,
                  "paymentModel": "TEAM",
                  "priceInCents": 12500,
                  "competitionFormat": "GROUP_STAGE",
                  "swissRounds": null,
                  "matchDurationMinutes": 40,
                  "intervalBetweenMatchesMinutes": 10,
                  "minimumRestMinutes": 60,
                  "schedulingStrategy": "BALANCED_REST"
                }
                """.formatted(name, maxTeams);
    }

    private static String uniqueEmail() {
        return "registration-" + UUID.randomUUID() + "@example.test";
    }
}
