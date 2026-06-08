package com.example.meteonode;

import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class NodeManagementIntegrationTest extends AbstractIntegrationTest {

    @Test
    void getAllNodes_asAdmin_returns200WithList() {
        String token = loginAs("admin", "admin123");
        provision("NODE-MGMT-ADMIN-001", "BMP280", "DHT11");

        var resp = rest.exchange("/api/nodes", HttpMethod.GET, bearer(token), JsonNode.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().isArray()).isTrue();
        assertThat(resp.getBody().size()).isPositive();
    }

    @Test
    void getAllNodes_asOperator_returns200() {
        String token = loginAs("operator", "admin123");

        var resp = rest.exchange("/api/nodes", HttpMethod.GET, bearer(token), JsonNode.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().isArray()).isTrue();
    }

    @Test
    void getAllNodes_asUser_returns200() {
        String token = loginAs("viewer", "admin123");

        var resp = rest.exchange("/api/nodes", HttpMethod.GET, bearer(token), JsonNode.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().isArray()).isTrue();
    }

    @Test
    void getAllNodes_unauthenticated_returns401() {
        var resp = rest.getForEntity("/api/nodes", String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getAllNodes_containsProvisionedNode() {
        String serial = "NODE-MGMT-FIND-001";
        String token = loginAs("admin", "admin123");
        int nodeId = provision(serial, "BMP280", "DHT11").get("nodeId").asInt();

        var resp = rest.exchange("/api/nodes", HttpMethod.GET, bearer(token), JsonNode.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(containsId(resp.getBody(), nodeId)).isTrue();
    }
}
