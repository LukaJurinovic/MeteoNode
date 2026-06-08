package com.example.meteonode;

import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class AlarmRuleIntegrationTest extends AbstractIntegrationTest {

    private static final String SERIAL = "NODE-RULE-TEST-001";

    private int createRule(int sensorId, int maxTemp, int cooldown, String token) {
        var resp = rest.exchange("/api/alarm-rules", HttpMethod.POST,
                jsonWithBearer("""
                        {"sensorId":%d,"metric":"TEMPERATURE","thresholdMax":%d,"severity":"WARNING","cooldownSeconds":%d}
                        """.formatted(sensorId, maxTemp, cooldown), token),
                JsonNode.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().get("id").asInt();
    }

    private void ingest(int nodeId, int sensorId, double value) {
        rest.postForEntity("/api/gateway/measurements",
                jsonWithApiKey("""
                        {"nodeId":%d,"measurements":[
                          {"sensorId":%d,"metric":"TEMPERATURE","value":%s,"measuredAt":"2025-06-01T10:00:00Z"}
                        ]}
                        """.formatted(nodeId, sensorId, value), API_KEY),
                Void.class);
    }

    @Test
    void createRule_appearsInList() {
        int sensorId = provision(SERIAL + "-LIST", "BMP280").get("sensorIds").get("BMP280").asInt();
        String token = loginAs("admin", "admin123");

        int ruleId = createRule(sensorId, 30, 0, token);

        var list = rest.exchange("/api/alarm-rules/sensor/" + sensorId,
                HttpMethod.GET, bearer(token), JsonNode.class);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(containsId(list.getBody(), ruleId)).isTrue();
    }

    @Test
    void toggleRule_changesActiveState() {
        int sensorId = provision(SERIAL + "-TOGGLE", "BMP280").get("sensorIds").get("BMP280").asInt();
        String token = loginAs("admin", "admin123");
        int ruleId = createRule(sensorId, 30, 0, token);

        var toggled = rest.exchange("/api/alarm-rules/" + ruleId + "/toggle",
                HttpMethod.PATCH, bearer(token), JsonNode.class);
        assertThat(toggled.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(toggled.getBody().get("isActive").asBoolean()).isFalse();

        var reEnabled = rest.exchange("/api/alarm-rules/" + ruleId + "/toggle",
                HttpMethod.PATCH, bearer(token), JsonNode.class);
        assertThat(reEnabled.getBody().get("isActive").asBoolean()).isTrue();
    }

    @Test
    void deleteRule_returns204_thenNotFound() {
        int sensorId = provision(SERIAL + "-DEL", "BMP280").get("sensorIds").get("BMP280").asInt();
        String token = loginAs("admin", "admin123");
        int ruleId = createRule(sensorId, 30, 0, token);

        var del = rest.exchange("/api/alarm-rules/" + ruleId,
                HttpMethod.DELETE, bearer(token), Void.class);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        var get = rest.exchange("/api/alarm-rules/" + ruleId,
                HttpMethod.DELETE, bearer(token), String.class);
        assertThat(get.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void userRole_cannotCreateRule_returns403() {
        int sensorId = provision(SERIAL + "-ROLE", "BMP280").get("sensorIds").get("BMP280").asInt();
        String viewerToken = loginAs("viewer", "admin123");

        var resp = rest.exchange("/api/alarm-rules", HttpMethod.POST,
                jsonWithBearer("""
                        {"sensorId":%d,"metric":"TEMPERATURE","thresholdMax":30,"severity":"WARNING"}
                        """.formatted(sensorId), viewerToken),
                String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void cooldown_suppressesDuplicateNotifications() {
        JsonNode prov = provision(SERIAL + "-COOL", "BMP280");
        int nodeId   = prov.get("nodeId").asInt();
        int sensorId = prov.get("sensorIds").get("BMP280").asInt();
        String token = loginAs("admin", "admin123");

        createRule(sensorId, 25, 300, token);

        ingest(nodeId, sensorId, 50.0);
        int afterFirst = rest.exchange("/api/notifications", HttpMethod.GET, bearer(token), JsonNode.class).getBody().size();
        assertThat(afterFirst).isGreaterThan(0);

        ingest(nodeId, sensorId, 55.0);
        int afterSecond = rest.exchange("/api/notifications", HttpMethod.GET, bearer(token), JsonNode.class).getBody().size();
        assertThat(afterSecond).isEqualTo(afterFirst);
    }
}
