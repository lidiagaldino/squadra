package com.squadra.platform.api.v1.health;
import com.squadra.platform.domain.entities.*;
import com.squadra.platform.domain.valueobjects.*;
import com.squadra.platform.domain.enums.*;
import com.squadra.platform.domain.repositories.*;
import com.squadra.platform.domain.services.*;
import com.squadra.platform.domain.exceptions.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class HealthResourceTest {
    @Test
    void returns_a_non_sensitive_readiness_response() {
        given()
            .when().get("/api/v1/health")
            .then()
            .statusCode(200)
            .body("status", is("ready"))
            .body("service", is("squadra-api"));
    }
}
