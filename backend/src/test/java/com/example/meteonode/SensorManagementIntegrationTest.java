package com.example.meteonode;

import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class SensorManagementIntegrationTest extends AbstractIntegrationTest {

    @Test
    void getSensorsByNode_asAdmin_returns200WithSensors() {
        int nodeId = provision("NODE-SENSOR-LIST-001", "BMP280", "DHT11").get("nodeId").asInt();
        String token = loginAs("admin", "admin123");

        var resp = rest.exchange("/api/sensors/node/" + nodeId,
                HttpMethod.GET, bearer(token), JsonNode.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().isArray()).isTrue();
        assertThat(resp.getBody().size()).isEqualTo(2);
    }

    @Test
    void getSensorsByNode_asUser_returns403() {
        int nodeId = provision("NODE-SENSOR-LIST-002", "BMP280", "DHT11").get("nodeId").asInt();
        String token = loginAs("viewer", "admin123");

        var resp = rest.exchange("/api/sensors/node/" + nodeId,
                HttpMethod.GET, bearer(token), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void deactivate_thenActivate_togglesIsActive() {
        var prov = provision("NODE-SENSOR-TOGGLE-001", "BMP280", "DHT11");
        int nodeId = prov.get("nodeId").asInt();
        int sensorId = prov.get("sensorIds").get("BMP280").asInt();
        String token = loginAs("admin", "admin123");

        var deactivate = rest.exchange("/api/sensors/" + sensorId + "/deactivate",
                HttpMethod.PATCH, bearer(token), Void.class);
        assertThat(deactivate.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        var afterDeactivate = rest.exchange("/api/sensors/node/" + nodeId,
                HttpMethod.GET, bearer(token), JsonNode.class);
        boolean activeAfterDeactivate = findSensorActive(afterDeactivate.getBody(), sensorId);
        assertThat(activeAfterDeactivate).isFalse();

        var activate = rest.exchange("/api/sensors/" + sensorId + "/activate",
                HttpMethod.PATCH, bearer(token), Void.class);
        assertThat(activate.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        var afterActivate = rest.exchange("/api/sensors/node/" + nodeId,
                HttpMethod.GET, bearer(token), JsonNode.class);
        boolean activeAfterActivate = findSensorActive(afterActivate.getBody(), sensorId);
        assertThat(activeAfterActivate).isTrue();
    }

    @Test
    void deactivate_asOperator_returns204() {
        int sensorId = provision("NODE-SENSOR-TOGGLE-002", "BMP280", "DHT11").get("sensorIds").get("BMP280").asInt();
        String token = loginAs("operator", "admin123");

        var resp = rest.exchange("/api/sensors/" + sensorId + "/deactivate",
                HttpMethod.PATCH, bearer(token), Void.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void deactivate_asUser_returns403() {
        int sensorId = provision("NODE-SENSOR-TOGGLE-003", "BMP280", "DHT11").get("sensorIds").get("BMP280").asInt();
        String token = loginAs("viewer", "admin123");

        var resp = rest.exchange("/api/sensors/" + sensorId + "/deactivate",
                HttpMethod.PATCH, bearer(token), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void delete_asAdmin_returns204_thenNotFound() {
        int sensorId = provision("NODE-SENSOR-DEL-001", "BMP280", "DHT11").get("sensorIds").get("DHT11").asInt();
        String token = loginAs("admin", "admin123");

        var del = rest.exchange("/api/sensors/" + sensorId,
                HttpMethod.DELETE, bearer(token), Void.class);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        var get = rest.exchange("/api/sensors/" + sensorId,
                HttpMethod.DELETE, bearer(token), String.class);
        assertThat(get.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void delete_asOperator_returns403() {
        int sensorId = provision("NODE-SENSOR-DEL-002", "BMP280", "DHT11").get("sensorIds").get("BMP280").asInt();
        String token = loginAs("operator", "admin123");

        var resp = rest.exchange("/api/sensors/" + sensorId,
                HttpMethod.DELETE, bearer(token), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void delete_asUser_returns403() {
        int sensorId = provision("NODE-SENSOR-DEL-003", "BMP280", "DHT11").get("sensorIds").get("BMP280").asInt();
        String token = loginAs("viewer", "admin123");

        var resp = rest.exchange("/api/sensors/" + sensorId,
                HttpMethod.DELETE, bearer(token), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private boolean findSensorActive(JsonNode array, int sensorId) {
        for (JsonNode s : array) {
            if (s.get("id").asInt() == sensorId) return s.get("isActive").asBoolean();
        }
        throw new AssertionError("Sensor " + sensorId + " not found in node sensor list");
    }
}
