package com.example.meteonode;

import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class MeasurementIngestionIntegrationTest extends AbstractIntegrationTest {

    private static final String SERIAL = "NODE-INGEST-TEST-001";

    @Test
    void ingestMeasurements_validBatch_returns204() {
        JsonNode prov = provision(SERIAL, "BMP280", "DHT11");
        int nodeId = prov.get("nodeId").asInt();
        int bmpId  = prov.get("sensorIds").get("BMP280").asInt();
        int dhtId  = prov.get("sensorIds").get("DHT11").asInt();

        var resp = rest.postForEntity("/api/gateway/measurements",
                jsonWithApiKey("""
                        {
                          "nodeId": %d,
                          "measurements": [
                            {"sensorId":%d,"metric":"TEMPERATURE","value":22.5,"measuredAt":"2025-01-01T12:00:00Z"},
                            {"sensorId":%d,"metric":"HUMIDITY","value":55.0,"measuredAt":"2025-01-01T12:00:00Z"}
                          ]
                        }
                        """.formatted(nodeId, bmpId, dhtId), API_KEY),
                Void.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void ingestMeasurements_aboveThreshold_triggersAlarmNotification() {
        JsonNode prov = provision(SERIAL + "-ALARM", "BMP280");
        int nodeId = prov.get("nodeId").asInt();
        int bmpId  = prov.get("sensorIds").get("BMP280").asInt();
        String adminToken = loginAs("admin", "admin123");

        rest.exchange("/api/alarm-rules", HttpMethod.POST,
                jsonWithBearer("""
                        {"sensorId":%d,"metric":"TEMPERATURE","thresholdMax":25,"severity":"WARNING","cooldownSeconds":0}
                        """.formatted(bmpId), adminToken),
                Void.class);

        rest.postForEntity("/api/gateway/measurements",
                jsonWithApiKey("""
                        {
                          "nodeId": %d,
                          "measurements": [
                            {"sensorId":%d,"metric":"TEMPERATURE","value":50.0,"measuredAt":"2025-01-01T12:00:00Z"}
                          ]
                        }
                        """.formatted(nodeId, bmpId), API_KEY),
                Void.class);

        var notifResp = rest.exchange("/api/notifications", HttpMethod.GET, bearer(adminToken), String.class);
        assertThat(notifResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(notifResp.getBody()).contains("TEMPERATURE");
    }

    @Test
    void ingest_unknownNode_returns404() {
        var resp = rest.postForEntity("/api/gateway/measurements",
                jsonWithApiKey("""
                        {
                          "nodeId": 999999,
                          "measurements": [
                            {"sensorId":1,"metric":"TEMPERATURE","value":22.5,"measuredAt":"2025-01-01T12:00:00Z"}
                          ]
                        }
                        """, API_KEY),
                Void.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void ingest_sensorNotOnNode_returns404() {
        int node1Id       = provision(SERIAL + "-CROSS1", "BMP280").get("nodeId").asInt();
        int node2SensorId = provision(SERIAL + "-CROSS2", "DHT11").get("sensorIds").get("DHT11").asInt();

        var resp = rest.postForEntity("/api/gateway/measurements",
                jsonWithApiKey("""
                        {
                          "nodeId": %d,
                          "measurements": [
                            {"sensorId":%d,"metric":"HUMIDITY","value":55.0,"measuredAt":"2025-01-01T12:00:00Z"}
                          ]
                        }
                        """.formatted(node1Id, node2SensorId), API_KEY),
                Void.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
