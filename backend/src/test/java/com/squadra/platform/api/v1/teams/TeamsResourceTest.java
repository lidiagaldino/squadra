package com.squadra.platform.api.v1.teams;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
class TeamsResourceTest {
    @Test
    void createsTeamWithOwnerButNoAthleteMembership() {
        String ownerToken = accessToken("ATHLETE");

        Response created = given().auth().oauth2(ownerToken)
                .contentType(ContentType.JSON)
                .body(teamBody("Liga Miga"))
                .when().post("/api/v1/teams");

        created.then().statusCode(201)
                .header("Location", notNullValue())
                .body("id", notNullValue())
                .body("name", equalTo("Liga Miga"))
                .body("administrationRole", equalTo("TEAM_OWNER"))
                .body("athleteMembershipStatus", equalTo(null))
                .body("administrators.role", hasItem("TEAM_OWNER"))
                .body("athletes.size()", equalTo(0));
    }

    @Test
    void listsTeamRelationshipsWithIndependentAdminAndAthleteFields() {
        String ownerToken = accessToken("ATHLETE");
        String teamId = createTeam(ownerToken, "Liga Miga");

        given().auth().oauth2(ownerToken)
                .when().get("/api/v1/teams")
                .then().statusCode(200)
                .body("id", hasItem(teamId))
                .body("administrationRole", hasItem("TEAM_OWNER"));

        given().auth().oauth2(ownerToken)
                .when().post("/api/v1/teams/{teamId}/athletes/me", teamId)
                .then().statusCode(200)
                .body("administrationRole", equalTo("TEAM_OWNER"))
                .body("athleteMembershipStatus", equalTo("ACTIVE"));
    }

    @Test
    void supportsInvitationAcceptanceAndConflictStates() {
        String ownerToken = accessToken("ATHLETE");
        String athleteEmail = uniqueEmail();
        register(athleteEmail, "ATHLETE").then().statusCode(201);
        String athleteToken = login(athleteEmail).jsonPath().getString("accessToken");
        String teamId = createTeam(ownerToken, "Liga Miga");

        given().auth().oauth2(ownerToken)
                .contentType(ContentType.JSON)
                .body("{\"email\":\"%s\"}".formatted(athleteEmail))
                .when().post("/api/v1/teams/{teamId}/athlete-invitations", teamId)
                .then().statusCode(200)
                .body("athletes.find { it.email == '%s' }.status".formatted(athleteEmail), equalTo("INVITED"));

        given().auth().oauth2(ownerToken)
                .contentType(ContentType.JSON)
                .body("{\"email\":\"%s\"}".formatted(athleteEmail))
                .when().post("/api/v1/teams/{teamId}/athlete-invitations", teamId)
                .then().statusCode(409)
                .body("code", equalTo("TEAM_MEMBERSHIP_CONFLICT"));

        given().auth().oauth2(athleteToken)
                .when().post("/api/v1/teams/{teamId}/athlete-invitations/accept", teamId)
                .then().statusCode(200)
                .body("athleteMembershipStatus", equalTo("ACTIVE"));

        given().auth().oauth2(athleteToken)
                .when().post("/api/v1/teams/{teamId}/athlete-invitations/accept", teamId)
                .then().statusCode(409);
    }

    @Test
    void ownerAndAdminCanManageRosterButOnlyOwnerManagesAdministrationAndTransfer() {
        String ownerEmail = uniqueEmail();
        String adminEmail = uniqueEmail();
        String athleteEmail = uniqueEmail();
        register(ownerEmail, "ATHLETE").then().statusCode(201);
        register(adminEmail, "ATHLETE").then().statusCode(201);
        register(athleteEmail, "ATHLETE").then().statusCode(201);
        String ownerToken = login(ownerEmail).jsonPath().getString("accessToken");
        String adminToken = login(adminEmail).jsonPath().getString("accessToken");
        String athleteToken = login(athleteEmail).jsonPath().getString("accessToken");
        String teamId = createTeam(ownerToken, "Liga Miga");

        given().auth().oauth2(ownerToken)
                .contentType(ContentType.JSON)
                .body("{\"email\":\"%s\",\"role\":\"TEAM_ADMIN\"}".formatted(adminEmail))
                .when().put("/api/v1/teams/{teamId}/admins", teamId)
                .then().statusCode(200)
                .body("administrators.find { it.email == '%s' }.role".formatted(adminEmail), equalTo("TEAM_ADMIN"));

        given().auth().oauth2(adminToken)
                .contentType(ContentType.JSON)
                .body(teamBody("Admin Edit"))
                .when().put("/api/v1/teams/{teamId}", teamId)
                .then().statusCode(200)
                .body("name", equalTo("Admin Edit"));

        given().auth().oauth2(adminToken)
                .contentType(ContentType.JSON)
                .body("{\"email\":\"%s\",\"role\":\"TEAM_ADMIN\"}".formatted(athleteEmail))
                .when().put("/api/v1/teams/{teamId}/admins", teamId)
                .then().statusCode(403);

        inviteAndAccept(ownerToken, athleteToken, teamId, athleteEmail);
        String athleteUserId = given().auth().oauth2(ownerToken).when().get("/api/v1/teams/{teamId}", teamId)
                .then().statusCode(200).extract().jsonPath().getString("athletes.find { it.email == '%s' }.userId".formatted(athleteEmail));

        given().auth().oauth2(ownerToken)
                .contentType(ContentType.JSON)
                .body("{\"userId\":\"%s\"}".formatted(athleteUserId))
                .when().put("/api/v1/teams/{teamId}/owner", teamId)
                .then().statusCode(200)
                .body("administrationRole", equalTo("TEAM_ADMIN"))
                .body("administrators.find { it.email == '%s' }.role".formatted(athleteEmail), equalTo("TEAM_OWNER"))
                .body("athletes.find { it.email == '%s' }.status".formatted(athleteEmail), equalTo("ACTIVE"));

        given().auth().oauth2(ownerToken)
                .contentType(ContentType.JSON)
                .body("{\"email\":\"%s\",\"role\":\"TEAM_ADMIN\"}".formatted(athleteEmail))
                .when().put("/api/v1/teams/{teamId}/admins", teamId)
                .then().statusCode(403);
    }

    @Test
    void rejectsUnauthenticatedUnrelatedInvalidAndTournamentRoleRequests() {
        String ownerToken = accessToken("ATHLETE");
        String otherToken = accessToken("ORGANIZER");
        String teamId = createTeam(ownerToken, "Liga Miga");

        given().contentType(ContentType.JSON).body(teamBody("No Auth"))
                .when().post("/api/v1/teams")
                .then().statusCode(401);

        given().auth().oauth2(otherToken)
                .when().get("/api/v1/teams/{teamId}", teamId)
                .then().statusCode(404);

        given().auth().oauth2(ownerToken).contentType(ContentType.JSON).body(teamBody("L"))
                .when().post("/api/v1/teams")
                .then().statusCode(422);

        given().auth().oauth2(ownerToken).contentType(ContentType.JSON)
                .body("{\"name\":\"Liga Extra\",\"unexpected\":\"nope\"}")
                .when().post("/api/v1/teams")
                .then().statusCode(422);

        given().auth().oauth2(ownerToken).contentType(ContentType.JSON).body("{\"email\":\"missing@example.test\",\"role\":\"CAPTAIN\"}")
                .when().put("/api/v1/teams/{teamId}/admins", teamId)
                .then().statusCode(422);

        given().auth().oauth2(ownerToken).contentType(ContentType.JSON).body("{\"email\":\"missing@example.test\"}")
                .when().post("/api/v1/teams/{teamId}/athlete-invitations", teamId)
                .then().statusCode(404)
                .body("code", equalTo("TEAM_TARGET_USER_NOT_FOUND"));
    }

    @Test
    void returnsEmptyListWhenUserHasNoTeams() {
        given().auth().oauth2(accessToken("ATHLETE"))
                .when().get("/api/v1/teams")
                .then().statusCode(200)
                .body("size()", equalTo(0));
    }

    private static void inviteAndAccept(String ownerToken, String athleteToken, String teamId, String athleteEmail) {
        given().auth().oauth2(ownerToken).contentType(ContentType.JSON).body("{\"email\":\"%s\"}".formatted(athleteEmail))
                .when().post("/api/v1/teams/{teamId}/athlete-invitations", teamId)
                .then().statusCode(200);
        given().auth().oauth2(athleteToken)
                .when().post("/api/v1/teams/{teamId}/athlete-invitations/accept", teamId)
                .then().statusCode(200);
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

    private static String uniqueEmail() {
        return "team-" + UUID.randomUUID() + "@example.test";
    }
}
