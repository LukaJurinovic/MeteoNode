package com.example.meteonode;

import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class StationManagementIntegrationTest extends AbstractIntegrationTest {

    private String createStation(String name, String token) {
        var resp = rest.exchange("/api/stations", HttpMethod.POST,
                jsonWithBearer("""
                        {"name":"%s"}
                        """.formatted(name), token),
                JsonNode.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().get("id").asText();
    }

    @Test
    void adminCanCreateStation() {
        String token = loginAs("admin", "admin123");
        assertThat(createStation("Integration Test Station", token)).isNotBlank();
    }

    @Test
    void operatorCannotCreateStation_returns403() {
        String token = loginAs("operator", "admin123");
        var resp = rest.exchange("/api/stations", HttpMethod.POST,
                jsonWithBearer("""
                        {"name":"Forbidden Station"}
                        """, token),
                String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void userCannotCreateStation_returns403() {
        String token = loginAs("viewer", "admin123");
        var resp = rest.exchange("/api/stations", HttpMethod.POST,
                jsonWithBearer("""
                        {"name":"Forbidden Station"}
                        """, token),
                String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void operatorCanUpdateStation() {
        String adminToken    = loginAs("admin", "admin123");
        String operatorToken = loginAs("operator", "admin123");
        String id = createStation("Before Update", adminToken);

        var resp = rest.exchange("/api/stations/" + id, HttpMethod.PUT,
                jsonWithBearer("""
                        {"name":"After Update"}
                        """, operatorToken),
                JsonNode.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().get("name").asText()).isEqualTo("After Update");
    }

    @Test
    void adminCanDeleteStation_thenNotFound() {
        String token = loginAs("admin", "admin123");
        String id = createStation("To Be Deleted", token);

        var del = rest.exchange("/api/stations/" + id, HttpMethod.DELETE, bearer(token), Void.class);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        var get = rest.exchange("/api/stations/" + id + "/overview", HttpMethod.GET, bearer(token), String.class);
        assertThat(get.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void listStations_unauthenticated_returns401() {
        var resp = rest.getForEntity("/api/stations", String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
