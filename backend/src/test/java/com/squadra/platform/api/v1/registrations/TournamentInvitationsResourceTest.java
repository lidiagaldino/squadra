package com.squadra.platform.api.v1.registrations;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
class TournamentInvitationsResourceTest {
    @jakarta.inject.Inject javax.sql.DataSource dataSource;
    @jakarta.inject.Inject com.squadra.platform.domain.repositories.TournamentInvitationRepository invitationRepository;
    @Test
    void ownerInvitesTeamAndAdminAcceptsIntoPrivateTournament() {
        String organizer = token("ORGANIZER");
        String owner = token("ATHLETE");
        String stranger = token("ATHLETE");
        String teamId = createTeam(owner, "Convidadas FC");
        String tournamentId = createPrivateOpenTournament(organizer, "Copa Privada Aceite");
        String categoryId = createCategory(organizer, tournamentId, "Adulto", 4);

        given().auth().oauth2(organizer).when().get("/api/v1/teams/directory?query=Convidadas")
                .then().statusCode(200).body("id", hasItem(teamId)).body("name", hasItem("Convidadas FC"));
        given().auth().oauth2(stranger).when().get("/api/v1/teams/directory")
                .then().statusCode(403);

        String invitationId = given().auth().oauth2(organizer).contentType(ContentType.JSON)
                .body("{\"teamId\":\"%s\"}".formatted(teamId))
                .when().post("/api/v1/tournaments/{tournamentId}/categories/{categoryId}/invitations", tournamentId, categoryId)
                .then().statusCode(201).header("Location", notNullValue())
                .body("status", equalTo("PENDING")).body("teamId", equalTo(teamId))
                .extract().jsonPath().getString("id");

        given().auth().oauth2(owner).when().get("/api/v1/tournaments/{tournamentId}/overview", tournamentId)
                .then().statusCode(200).body("tournament.visibility", equalTo("PRIVATE"))
                .body("categories.id", hasItem(categoryId));
        given().auth().oauth2(stranger).when().get("/api/v1/tournaments/{tournamentId}/overview", tournamentId)
                .then().statusCode(404);
        given().auth().oauth2(owner).when().get("/api/v1/teams/{teamId}/tournament-invitations", teamId)
                .then().statusCode(200).body("id", hasItem(invitationId));

        given().auth().oauth2(owner).when().post("/api/v1/teams/{teamId}/tournament-invitations/{id}/accept", teamId, invitationId)
                .then().statusCode(200).body("status", equalTo("ACCEPTED"));
        given().auth().oauth2(owner).when().get("/api/v1/teams/{teamId}/registrations", teamId)
                .then().statusCode(200).body("categoryId", hasItem(categoryId)).body("status", hasItem("DRAFT"));
        given().auth().oauth2(owner).when().post("/api/v1/teams/{teamId}/tournament-invitations/{id}/accept", teamId, invitationId)
                .then().statusCode(409);
        given().auth().oauth2(owner).get("/api/v1/teams/{team}/registrations", teamId)
                .then().statusCode(200).body("size()", equalTo(1));
        given().auth().oauth2(owner).contentType(ContentType.JSON).body("{\"teamId\":\"%s\"}".formatted(teamId))
                .when().post("/api/v1/tournaments/{tournamentId}/categories/{categoryId}/registrations", tournamentId, categoryId)
                .then().statusCode(404);
    }

    @Test
    void rejectsDuplicateInvitationAndAllowsNewInvitationAfterRejection() {
        String organizer = token("ORGANIZER");
        String owner = token("ATHLETE");
        String otherOrganizer = token("ORGANIZER");
        String teamId = createTeam(owner, "Recusadas FC");
        String tournamentId = createPrivateOpenTournament(organizer, "Copa Privada Recusa");
        String categoryId = createCategory(organizer, tournamentId, "Sub 20", 4);
        String route = "/api/v1/tournaments/{tournamentId}/categories/{categoryId}/invitations";
        String body = "{\"teamId\":\"%s\"}".formatted(teamId);
        String invitationId = given().auth().oauth2(organizer).contentType(ContentType.JSON).body(body)
                .when().post(route, tournamentId, categoryId).then().statusCode(201).extract().jsonPath().getString("id");

        given().auth().oauth2(organizer).contentType(ContentType.JSON).body(body)
                .when().post(route, tournamentId, categoryId).then().statusCode(409);
        given().auth().oauth2(otherOrganizer).when().get(route, tournamentId, categoryId).then().statusCode(404);
        given().auth().oauth2(owner).when().post("/api/v1/teams/{teamId}/tournament-invitations/{id}/reject", teamId, invitationId)
                .then().statusCode(200).body("status", equalTo("REJECTED"));

        given().auth().oauth2(organizer).contentType(ContentType.JSON).body(body)
                .when().post(route, tournamentId, categoryId).then().statusCode(201).body("status", equalTo("PENDING"));
    }

    @Test
    void fullCategoryLeavesInvitationPendingAndOwnerCanCancel() {
        String organizer = token("ORGANIZER");
        String firstOwner = token("ATHLETE");
        String secondOwner = token("ATHLETE");
        String firstTeam = createTeam(firstOwner, "Primeiras Convidadas");
        String secondTeam = createTeam(secondOwner, "Segundas Convidadas");
        String tournamentId = createPrivateOpenTournament(organizer, "Copa Privada Lotada");
        String categoryId = createCategory(organizer, tournamentId, "Livre", 2);
        String route = "/api/v1/tournaments/{tournamentId}/categories/{categoryId}/invitations";
        String firstInvite = invite(organizer, tournamentId, categoryId, firstTeam);
        String secondInvite = invite(organizer, tournamentId, categoryId, secondTeam);

        given().auth().oauth2(firstOwner).when().post("/api/v1/teams/{teamId}/tournament-invitations/{id}/accept", firstTeam, firstInvite)
                .then().statusCode(200);
        String fillerOwner = token("ATHLETE");
        String fillerTeam = createTeam(fillerOwner, "Ultima Vaga");
        String fillerInvite = invite(organizer, tournamentId, categoryId, fillerTeam);
        given().auth().oauth2(fillerOwner).when().post("/api/v1/teams/{teamId}/tournament-invitations/{id}/accept", fillerTeam, fillerInvite)
                .then().statusCode(200);
        given().auth().oauth2(secondOwner).when().post("/api/v1/teams/{teamId}/tournament-invitations/{id}/accept", secondTeam, secondInvite)
                .then().statusCode(409);
        given().auth().oauth2(secondOwner).when().get("/api/v1/teams/{teamId}/tournament-invitations", secondTeam)
                .then().statusCode(200).body("id", hasItem(secondInvite)).body("status", hasItem("PENDING"));
        given().auth().oauth2(organizer).when().post(route + "/{invitationId}/cancel", tournamentId, categoryId, secondInvite)
                .then().statusCode(200).body("status", equalTo("CANCELLED"));
        given().auth().oauth2(secondOwner).get("/api/v1/teams/{team}/registrations", secondTeam)
                .then().statusCode(200).body("size()", equalTo(0));
        given().auth().oauth2(secondOwner).get("/api/v1/tournaments/{id}/overview", tournamentId)
                .then().statusCode(404);
    }

    @Test
    void invitationEndpointsEnforceAuthenticationScopeAndDecisions() {
        String organizer = token("ORGANIZER");
        String other = token("ORGANIZER");
        String owner = token("ATHLETE");
        String team = createTeam(owner, "Privacidade FC");
        String tournament = createPrivateOpenTournament(organizer, "Copa Protegida");
        String category = createCategory(organizer, tournament, "Livre", 2);
        String base = "/api/v1/tournaments/" + tournament + "/categories/" + category + "/invitations";
        String inbox = "/api/v1/teams/" + team + "/tournament-invitations";
        String body = "{\"teamId\":\"%s\"}".formatted(team);
        given().when().get(base).then().statusCode(401);
        given().contentType(ContentType.JSON).body(body).post(base).then().statusCode(401);
        given().when().get(inbox).then().statusCode(401);
        given().auth().oauth2(other).contentType(ContentType.JSON).body(body).post(base).then().statusCode(404);
        given().auth().oauth2(organizer).contentType(ContentType.JSON).body("{}").post(base).then().statusCode(422);
        given().auth().oauth2(organizer).get("/api/v1/teams/directory?size=51").then().statusCode(422);
        String invitation = invite(organizer, tournament, category, team);
        given().auth().oauth2(organizer).get(base).then().statusCode(200).body("id", hasItem(invitation));
        given().auth().oauth2(other).get(inbox).then().statusCode(403);
        for (String action : new String[]{"accept", "reject"}) {
            given().post(inbox + "/" + invitation + "/" + action).then().statusCode(401);
            given().auth().oauth2(other).post(inbox + "/" + invitation + "/" + action).then().statusCode(403);
            given().auth().oauth2(owner).post(inbox + "/" + UUID.randomUUID() + "/" + action).then().statusCode(404);
        }
        given().post(base + "/" + invitation + "/cancel").then().statusCode(401);
        given().auth().oauth2(other).post(base + "/" + invitation + "/cancel").then().statusCode(404);
        given().auth().oauth2(organizer).post("/api/v1/tournaments/" + tournament + "/close").then().statusCode(200);
        given().auth().oauth2(owner).post(inbox + "/" + invitation + "/accept").then().statusCode(409);
        given().auth().oauth2(owner).get(inbox).then().statusCode(200).body("status", hasItem("PENDING"));
        given().auth().oauth2(owner).post(inbox + "/" + invitation + "/reject").then().statusCode(200).body("status", equalTo("REJECTED"));
        given().auth().oauth2(owner).post(inbox + "/" + invitation + "/reject").then().statusCode(409);
        given().auth().oauth2(owner).get("/api/v1/teams/" + team + "/registrations").then().statusCode(200).body("size()", equalTo(0));
    }

    @Test
    void concurrentAcceptsCannotExceedLastSlotOrCreateDuplicateRegistration() {
        String organizer = token("ORGANIZER");
        String owner = token("ATHLETE");
        String tournament = createPrivateOpenTournament(organizer, "Copa Concorrente Privada");
        String category = createCategory(organizer, tournament, "Livre", 2);
        String[] teams = new String[3];
        String[] invites = new String[3];
        for (int i = 0; i < 3; i++) {
            teams[i] = createTeam(owner, "Concorrente " + i);
            invites[i] = invite(organizer, tournament, category, teams[i]);
        }
        String route = "/api/v1/teams/{team}/tournament-invitations/{invite}/accept";
        given().auth().oauth2(owner).post(route, teams[0], invites[0]).then().statusCode(200);
        CompletableFuture<Integer> first = CompletableFuture.supplyAsync(() -> given().auth().oauth2(owner).post(route, teams[1], invites[1]).statusCode());
        CompletableFuture<Integer> second = CompletableFuture.supplyAsync(() -> given().auth().oauth2(owner).post(route, teams[2], invites[2]).statusCode());
        assertEquals(java.util.Set.of(200, 409), java.util.Set.of(first.join(), second.join()));
        given().auth().oauth2(organizer).get("/api/v1/tournaments/{t}/categories/{c}/registrations", tournament, category)
                .then().statusCode(200).body("size()", equalTo(2));
    }

    @Test
    void databaseRejectsNullMarkersForPendingInvitationsAndActiveRegistrations() throws Exception {
        String organizer = token("ORGANIZER");
        String owner = token("ATHLETE");
        String team = createTeam(owner, "Integridade FC");
        String tournament = createPrivateOpenTournament(organizer, "Copa Integridade");
        String category = createCategory(organizer, tournament, "Livre", 2);
        String invitation = invite(organizer, tournament, category, team);
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(
                "UPDATE squadra.tournament_invitations SET pending_key = NULL WHERE id = ?")) {
            statement.setObject(1, UUID.fromString(invitation));
            var failure = org.junit.jupiter.api.Assertions.assertThrows(java.sql.SQLException.class, statement::executeUpdate);
            assertEquals(connection.getMetaData().getDatabaseProductName().equals("PostgreSQL") ? "23514" : "23513", failure.getSQLState());
        }
        given().auth().oauth2(owner).post("/api/v1/teams/{team}/tournament-invitations/{invite}/accept", team, invitation)
                .then().statusCode(200);
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(
                "UPDATE squadra.tournament_registrations SET active_key = NULL WHERE category_id = ?")) {
            statement.setObject(1, UUID.fromString(category));
            var failure = org.junit.jupiter.api.Assertions.assertThrows(java.sql.SQLException.class, statement::executeUpdate);
            assertEquals(connection.getMetaData().getDatabaseProductName().equals("PostgreSQL") ? "23514" : "23513", failure.getSQLState());
        }
    }

    @Test
    void rejectsInvalidVisibilityAndInvitationStateAndCategoryScope() {
        String organizer = token("ORGANIZER");
        String owner = token("ATHLETE");
        String team = createTeam(owner, "Contexto Convite");
        given().auth().oauth2(organizer).contentType(ContentType.JSON)
                .body(tournamentBody("Visibilidade Invalida").replace("PRIVATE", "SECRET"))
                .post("/api/v1/tournaments").then().statusCode(422);
        String draft = given().auth().oauth2(organizer).contentType(ContentType.JSON)
                .body(tournamentBody("Copa Rascunho")).post("/api/v1/tournaments")
                .then().statusCode(201).extract().path("id");
        String category = createCategory(organizer, draft, "Livre", 2);
        String route = "/api/v1/tournaments/{t}/categories/{c}/invitations";
        String body = "{\"teamId\":\"%s\"}".formatted(team);
        given().auth().oauth2(organizer).contentType(ContentType.JSON).body(body).post(route, draft, category).then().statusCode(409);
        String publicId = given().auth().oauth2(organizer).contentType(ContentType.JSON)
                .body(tournamentBody("Copa Publica").replace("PRIVATE", "PUBLIC")).post("/api/v1/tournaments")
                .then().statusCode(201).extract().path("id");
        String publicCategory = createCategory(organizer, publicId, "Livre", 2);
        given().auth().oauth2(organizer).post("/api/v1/tournaments/{id}/publish", publicId).then().statusCode(200);
        given().auth().oauth2(organizer).contentType(ContentType.JSON).body(body).post(route, publicId, publicCategory).then().statusCode(409);
        String privateId = createPrivateOpenTournament(organizer, "Copa Escopo");
        given().auth().oauth2(organizer).contentType(ContentType.JSON).body(body).post(route, privateId, category).then().statusCode(404);
        given().auth().oauth2(organizer).get(route, privateId, category).then().statusCode(404);
        String privateCategory = createCategory(organizer, privateId, "Livre", 2);
        String accepted = invite(organizer, privateId, privateCategory, team);
        given().auth().oauth2(owner).post("/api/v1/teams/{team}/tournament-invitations/{id}/accept", team, accepted).then().statusCode(200);
        given().auth().oauth2(organizer).contentType(ContentType.JSON).body(body).post(route, privateId, privateCategory).then().statusCode(409);
    }

    @Test
    void directoryIsBoundedAndMinimalAndInboxIsTeamScoped() {
        String organizer = token("ORGANIZER");
        String owner = token("ATHLETE");
        String prefix = "Directory " + UUID.randomUUID().toString().substring(0, 8);
        String first = createTeam(owner, prefix + " A");
        String second = createTeam(owner, prefix + " B");
        var page = given().auth().oauth2(organizer).queryParam("query", prefix).queryParam("size", 1)
                .get("/api/v1/teams/directory").then().statusCode(200).body("size()", equalTo(1)).extract().jsonPath();
        java.util.Map<String, Object> item = page.getMap("[0]");
        assertEquals(java.util.Set.of("id", "name", "city", "state"), item.keySet());
        assertEquals("Sao Paulo", item.get("city"));
        assertEquals("SP", item.get("state"));
        String secondPageId = given().auth().oauth2(organizer).queryParam("query", prefix).queryParam("size", 1).queryParam("page", 1)
                .get("/api/v1/teams/directory").then().statusCode(200).body("size()", equalTo(1)).extract().path("[0].id");
        org.junit.jupiter.api.Assertions.assertNotEquals(item.get("id"), secondPageId);
        given().auth().oauth2(organizer).queryParam("query", prefix).queryParam("page", 2).queryParam("size", 1)
                .get("/api/v1/teams/directory").then().statusCode(200).body("size()", equalTo(0));
        String tournament = createPrivateOpenTournament(organizer, "Copa Caixa Privada");
        String category = createCategory(organizer, tournament, "Livre", 4);
        String firstInvite = invite(organizer, tournament, category, first);
        String secondInvite = invite(organizer, tournament, category, second);
        given().auth().oauth2(owner).get("/api/v1/teams/{id}/tournament-invitations", first)
                .then().statusCode(200).body("size()", equalTo(1)).body("[0].id", equalTo(firstInvite))
                .body("[0].tournamentName", equalTo("Copa Caixa Privada")).body("[0].categoryName", equalTo("Livre"))
                .body("[0].teamId", equalTo(first)).body("[0].status", equalTo("PENDING"));
        given().auth().oauth2(owner).post("/api/v1/teams/{team}/tournament-invitations/{invite}/accept", first, secondInvite)
                .then().statusCode(404);
    }

    @Test
    void cancelRacingAcceptHasOneTerminalOutcomeWithMatchingRegistration() {
        String organizer = token("ORGANIZER");
        String owner = token("ATHLETE");
        String team = createTeam(owner, "Corrida Cancelamento");
        String tournament = createPrivateOpenTournament(organizer, "Copa Corrida Cancelamento");
        String category = createCategory(organizer, tournament, "Livre", 2);
        String invitation = invite(organizer, tournament, category, team);
        var accept = CompletableFuture.supplyAsync(() -> given().auth().oauth2(owner)
                .post("/api/v1/teams/{team}/tournament-invitations/{id}/accept", team, invitation).statusCode());
        var cancel = CompletableFuture.supplyAsync(() -> given().auth().oauth2(organizer)
                .post("/api/v1/tournaments/{t}/categories/{c}/invitations/{i}/cancel", tournament, category, invitation).statusCode());
        int accepted = accept.join();
        int cancelled = cancel.join();
        assertEquals(1, (accepted == 200 ? 1 : 0) + (cancelled == 200 ? 1 : 0));
        assertEquals(1, (accepted == 409 ? 1 : 0) + (cancelled == 409 ? 1 : 0));
        given().auth().oauth2(owner).get("/api/v1/teams/{team}/tournament-invitations", team)
                .then().statusCode(200).body("[0].status", equalTo(accepted == 200 ? "ACCEPTED" : "CANCELLED"));
        given().auth().oauth2(owner).get("/api/v1/teams/{team}/registrations", team)
                .then().statusCode(200).body("size()", equalTo(accepted == 200 ? 1 : 0));
    }

    @Test
    void teamAdminCanAcceptAndLosingAdministrationImmediatelyRevokesAccess() {
        String organizer = token("ORGANIZER");
        String owner = token("ATHLETE");
        String adminEmail = "admin-" + UUID.randomUUID() + "@example.test";
        given().contentType(ContentType.JSON).body("{\"email\":\"%s\",\"password\":\"correct horse battery\",\"role\":\"ATHLETE\"}".formatted(adminEmail))
                .post("/api/v1/auth/register").then().statusCode(201);
        String admin = given().contentType(ContentType.JSON).body("{\"email\":\"%s\",\"password\":\"correct horse battery\"}".formatted(adminEmail))
                .post("/api/v1/auth/login").then().statusCode(200).extract().path("accessToken");
        String team = createTeam(owner, "Administradoras FC");
        String adminId = given().auth().oauth2(owner).contentType(ContentType.JSON)
                .body("{\"email\":\"%s\",\"role\":\"TEAM_ADMIN\"}".formatted(adminEmail))
                .put("/api/v1/teams/{team}/admins", team).then().statusCode(200)
                .extract().path("administrators.find { it.role == 'TEAM_ADMIN' }.userId");
        String tournament = createPrivateOpenTournament(organizer, "Copa Admin");
        String category = createCategory(organizer, tournament, "Livre", 2);
        String invitation = invite(organizer, tournament, category, team);
        given().auth().oauth2(admin).get("/api/v1/teams/{team}/tournament-invitations", team)
                .then().statusCode(200).body("[0].id", equalTo(invitation));
        given().auth().oauth2(admin).post("/api/v1/teams/{team}/tournament-invitations/{id}/accept", team, invitation)
                .then().statusCode(200).body("status", equalTo("ACCEPTED"));
        given().auth().oauth2(admin).get("/api/v1/tournaments/{id}/overview", tournament).then().statusCode(200);
        given().auth().oauth2(owner).delete("/api/v1/teams/{team}/admins/{id}", team, adminId).then().statusCode(200);
        given().auth().oauth2(admin).get("/api/v1/teams/{team}/tournament-invitations", team).then().statusCode(403);
        given().auth().oauth2(admin).post("/api/v1/teams/{team}/tournament-invitations/{id}/reject", team, invitation).then().statusCode(403);
        given().auth().oauth2(admin).get("/api/v1/tournaments/{id}/overview", tournament).then().statusCode(404);
    }

    @Test
    void visibilityPersistsAndForeignOrPublishedEditsNeverChangeIt() {
        String organizer = token("ORGANIZER");
        String stranger = token("ORGANIZER");
        String body = tournamentBody("Copa Visibilidade");
        String tournament = given().auth().oauth2(organizer).contentType(ContentType.JSON).body(body)
                .post("/api/v1/tournaments").then().statusCode(201).extract().path("id");
        given().auth().oauth2(organizer).contentType(ContentType.JSON).body(body.replace("PRIVATE", "PUBLIC"))
                .put("/api/v1/tournaments/{id}", tournament).then().statusCode(200).body("visibility", equalTo("PUBLIC"));
        given().auth().oauth2(organizer).get("/api/v1/tournaments/{id}", tournament).then().statusCode(200).body("visibility", equalTo("PUBLIC"));
        given().auth().oauth2(organizer).contentType(ContentType.JSON).body(body)
                .put("/api/v1/tournaments/{id}", tournament).then().statusCode(200).body("visibility", equalTo("PRIVATE"));
        given().auth().oauth2(stranger).contentType(ContentType.JSON).body(body.replace("PRIVATE", "PUBLIC"))
                .put("/api/v1/tournaments/{id}", tournament).then().statusCode(404);
        given().auth().oauth2(organizer).post("/api/v1/tournaments/{id}/publish", tournament).then().statusCode(200);
        given().auth().oauth2(organizer).contentType(ContentType.JSON).body(body.replace("PRIVATE", "PUBLIC"))
                .put("/api/v1/tournaments/{id}", tournament).then().statusCode(409);
        given().auth().oauth2(organizer).get("/api/v1/tournaments/{id}", tournament).then().statusCode(200).body("visibility", equalTo("PRIVATE"));
        given().get("/api/v1/tournaments/{id}/overview", tournament).then().statusCode(401);
        given().get("/api/v1/tournaments/available").then().statusCode(401);
    }

    @Test
    void discoveryReturnsOnlyOpenPublicTournamentsInDistinctBoundedPages() {
        String organizer = token("ORGANIZER");
        String viewer = token("ATHLETE");
        java.util.List<String> ids = new java.util.ArrayList<>();
        for (String name : java.util.List.of("Descoberta Aberta A", "Descoberta Aberta B", "Descoberta Fechada", "Descoberta Rascunho")) {
            ids.add(given().auth().oauth2(organizer).contentType(ContentType.JSON)
                    .body(tournamentBody(name).replace("PRIVATE", "PUBLIC")).post("/api/v1/tournaments")
                    .then().statusCode(201).extract().path("id"));
        }
        for (String id : ids.subList(0, 3)) given().auth().oauth2(organizer).post("/api/v1/tournaments/{id}/publish", id).then().statusCode(200);
        given().auth().oauth2(organizer).post("/api/v1/tournaments/{id}/close", ids.get(2)).then().statusCode(200);
        String privateId = createPrivateOpenTournament(organizer, "Descoberta Privada");
        given().auth().oauth2(viewer).get("/api/v1/tournaments/available?size=50").then().statusCode(200)
                .body("id", hasItem(ids.get(0))).body("id", hasItem(ids.get(1)))
                .body("id", org.hamcrest.Matchers.not(hasItem(ids.get(2))))
                .body("id", org.hamcrest.Matchers.not(hasItem(ids.get(3))))
                .body("id", org.hamcrest.Matchers.not(hasItem(privateId)))
                .body("status", org.hamcrest.Matchers.everyItem(equalTo("OPEN")))
                .body("visibility", org.hamcrest.Matchers.everyItem(equalTo("PUBLIC")));
        String first = given().auth().oauth2(viewer).get("/api/v1/tournaments/available?size=1&page=0")
                .then().statusCode(200).body("size()", equalTo(1)).extract().path("[0].id");
        String second = given().auth().oauth2(viewer).get("/api/v1/tournaments/available?size=1&page=1")
                .then().statusCode(200).body("size()", equalTo(1)).extract().path("[0].id");
        org.junit.jupiter.api.Assertions.assertNotEquals(first, second);
        given().auth().oauth2(viewer).get("/api/v1/tournaments/available?size=1&page=100000")
                .then().statusCode(200).body("size()", equalTo(0));
    }

    @Test
    void inviteRacingAcceptanceCannotLeavePendingInvitationForRegisteredTeam() {
        String organizer = token("ORGANIZER");
        String owner = token("ATHLETE");
        String team = createTeam(owner, "Convite Concorrente FC");
        String tournament = createPrivateOpenTournament(organizer, "Copa Novo Convite Concorrente");
        String category = createCategory(organizer, tournament, "Livre", 2);
        String invitation = invite(organizer, tournament, category, team);
        var accept = CompletableFuture.supplyAsync(() -> given().auth().oauth2(owner)
                .post("/api/v1/teams/{team}/tournament-invitations/{id}/accept", team, invitation).statusCode());
        var invite = CompletableFuture.supplyAsync(() -> given().auth().oauth2(organizer).contentType(ContentType.JSON)
                .body("{\"teamId\":\"%s\"}".formatted(team))
                .post("/api/v1/tournaments/{t}/categories/{c}/invitations", tournament, category).statusCode());
        assertEquals(200, accept.join());
        assertEquals(409, invite.join());
        String organizerId = given().auth().oauth2(organizer).get("/api/v1/tournaments/{id}", tournament)
                .then().statusCode(200).extract().path("ownerOrganizerUserId");
        var stalePrecheckedInvite = com.squadra.platform.domain.entities.TournamentInvitation.create(
                UUID.fromString(tournament), UUID.fromString(category), UUID.fromString(team), UUID.fromString(organizerId), java.time.Instant.now());
        org.junit.jupiter.api.Assertions.assertThrows(com.squadra.platform.domain.exceptions.TournamentInvitationConflictException.class,
                () -> invitationRepository.create(stalePrecheckedInvite).await().indefinitely());
        given().auth().oauth2(owner).get("/api/v1/teams/{team}/tournament-invitations", team)
                .then().statusCode(200).body("size()", equalTo(1)).body("[0].status", equalTo("ACCEPTED"));
        given().auth().oauth2(owner).get("/api/v1/teams/{team}/registrations", team)
                .then().statusCode(200).body("size()", equalTo(1)).body("[0].status", equalTo("DRAFT"));
    }

    private static String invite(String organizer, String tournamentId, String categoryId, String teamId) {
        return given().auth().oauth2(organizer).contentType(ContentType.JSON).body("{\"teamId\":\"%s\"}".formatted(teamId))
                .when().post("/api/v1/tournaments/{tournamentId}/categories/{categoryId}/invitations", tournamentId, categoryId)
                .then().statusCode(201).extract().jsonPath().getString("id");
    }

    private static String createPrivateOpenTournament(String token, String name) {
        String tournamentId = given().auth().oauth2(token).contentType(ContentType.JSON)
                .body(tournamentBody(name))
                .when().post("/api/v1/tournaments").then().statusCode(201).extract().jsonPath().getString("id");
        given().auth().oauth2(token).when().post("/api/v1/tournaments/{id}/publish", tournamentId).then().statusCode(200);
        return tournamentId;
    }

    private static String createCategory(String token, String tournamentId, String name, int maxTeams) {
        return given().auth().oauth2(token).contentType(ContentType.JSON).body(categoryBody(name, maxTeams))
                .when().post("/api/v1/tournaments/{id}/categories", tournamentId)
                .then().statusCode(201).extract().jsonPath().getString("id");
    }

    private static String createTeam(String token, String name) {
        return given().auth().oauth2(token).contentType(ContentType.JSON).body("""
                {"name":"%s","city":"Sao Paulo","state":"SP","foundedYear":2020,"description":"Football team."}
                """.formatted(name)).when().post("/api/v1/teams").then().statusCode(201).extract().jsonPath().getString("id");
    }

    private static String token(String role) {
        String email = "invite-" + UUID.randomUUID() + "@example.test";
        given().contentType(ContentType.JSON).body("{\"email\":\"%s\",\"password\":\"correct horse battery\",\"role\":\"%s\"}".formatted(email, role))
                .when().post("/api/v1/auth/register").then().statusCode(201);
        return given().contentType(ContentType.JSON).body("{\"email\":\"%s\",\"password\":\"correct horse battery\"}".formatted(email))
                .when().post("/api/v1/auth/login").then().statusCode(200).extract().jsonPath().getString("accessToken");
    }

    private static String tournamentBody(String name) {
        return """
                {"name":"%s","visibility":"PRIVATE","description":"Private tournament.","city":"Sao Paulo","state":"SP","venueName":"Arena Central","startDate":"2026-10-10","endDate":"2026-10-12","registrationDeadline":"2026-10-01","rules":"Standard rules."}
                """.formatted(name);
    }

    private static String categoryBody(String name, int maxTeams) {
        return """
                {"name":"%s","modality":"FUTSAL","minAthletes":7,"maxAthletes":12,"minTeams":2,"maxTeams":%d,"paymentModel":"TEAM","priceInCents":12500,"competitionFormat":"GROUP_STAGE","swissRounds":null,"matchDurationMinutes":40,"intervalBetweenMatchesMinutes":10,"minimumRestMinutes":60,"schedulingStrategy":"BALANCED_REST"}
                """.formatted(name, maxTeams);
    }
}
