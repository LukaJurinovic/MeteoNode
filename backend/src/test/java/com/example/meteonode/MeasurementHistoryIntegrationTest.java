package com.example.meteonode;

import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class MeasurementHistoryIntegrationTest extends AbstractIntegrationTest {

    private static final String SERIAL = "NODE-HIST-TEST-001";

    @Test
    void ingestedMeasurements_areReturnedByHistoryEndpoint() {
        JsonNode prov = provision(SERIAL + "-BASIC", "BMP280");
        int nodeId   = prov.get("nodeId").asInt();
        int sensorId = prov.get("sensorIds").get("BMP280").asInt();

        for (int i = 1; i <= 3; i++) {
            rest.postForEntity("/api/gateway/measurements",
                    jsonWithApiKey("""
                            {"nodeId":%d,"measurements":[
                              {"sensorId":%d,"metric":"TEMPERATURE","value":%d.0,"measuredAt":"2025-03-01T%02d:00:00Z"}
                            ]}
                            """.formatted(nodeId, sensorId, 20 + i, i + 9), API_KEY),
                    Void.class);
        }

        String token = loginAs("viewer", "admin123");
        var resp = rest.exchange(
                "/api/measurements/history?sensorId=%d&metric=TEMPERATURE&from=2025-03-01T00:00:00Z&to=2025-03-02T00:00:00Z"
                        .formatted(sensorId),
                HttpMethod.GET, bearer(token), JsonNode.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode content = resp.getBody().get("content");
        assertThat(content.isArray()).isTrue();
        assertThat(content.size()).isEqualTo(3);
    }

    @Test
    void historyEndpoint_outsideRange_returnsEmpty() {
        JsonNode prov = provision(SERIAL + "-RANGE", "BMP280");
        int nodeId   = prov.get("nodeId").asInt();
        int sensorId = prov.get("sensorIds").get("BMP280").asInt();

        rest.postForEntity("/api/gateway/measurements",
                jsonWithApiKey("""
                        {"nodeId":%d,"measurements":[
                          {"sensorId":%d,"metric":"TEMPERATURE","value":22.0,"measuredAt":"2025-04-01T12:00:00Z"}
                        ]}
                        """.formatted(nodeId, sensorId), API_KEY),
                Void.class);

        String token = loginAs("viewer", "admin123");
        var resp = rest.exchange(
                "/api/measurements/history?sensorId=%d&metric=TEMPERATURE&from=2025-01-01T00:00:00Z&to=2025-02-01T00:00:00Z"
                        .formatted(sensorId),
                HttpMethod.GET, bearer(token), JsonNode.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().get("content").size()).isZero();
    }

    @Test
    void historyEndpoint_unauthenticated_returns401() {
        var resp = rest.getForEntity(
                "/api/measurements/history?sensorId=1&metric=TEMPERATURE&from=2025-01-01T00:00:00Z&to=2025-02-01T00:00:00Z",
                String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void historyEndpoint_pagination_respectsPageSize() {
        JsonNode prov = provision(SERIAL + "-PAGE", "BMP280");
        int nodeId   = prov.get("nodeId").asInt();
        int sensorId = prov.get("sensorIds").get("BMP280").asInt();

        for (int i = 1; i <= 5; i++) {
            rest.postForEntity("/api/gateway/measurements",
                    jsonWithApiKey("""
                            {"nodeId":%d,"measurements":[
                              {"sensorId":%d,"metric":"PRESSURE","value":%d.0,"measuredAt":"2025-05-01T%02d:00:00Z"}
                            ]}
                            """.formatted(nodeId, sensorId, 1000 + i, i), API_KEY),
                    Void.class);
        }

        String token = loginAs("viewer", "admin123");
        var resp = rest.exchange(
                "/api/measurements/history?sensorId=%d&metric=PRESSURE&from=2025-05-01T00:00:00Z&to=2025-05-02T00:00:00Z&page=0&size=2"
                        .formatted(sensorId),
                HttpMethod.GET, bearer(token), JsonNode.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().get("content").size()).isEqualTo(2);
        assertThat(resp.getBody().get("totalElements").asInt()).isEqualTo(5);
    }
}
