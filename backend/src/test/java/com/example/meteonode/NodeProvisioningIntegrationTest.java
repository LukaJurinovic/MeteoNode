package com.example.meteonode;

import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class NodeProvisioningIntegrationTest extends AbstractIntegrationTest {

    private static final String SERIAL = "NODE-PROV-TEST-001";

    @Test
    void provision_newNode_returns201WithSensorIds() {
        JsonNode body = provision(SERIAL, "BMP280", "DHT11", "GUVA_S12SD");

        assertThat(body.get("nodeId").isNumber()).isTrue();
        assertThat(body.get("nodeId").asInt()).isPositive();
        assertThat(body.get("sensorIds").get("BMP280").isNumber()).isTrue();
        assertThat(body.get("sensorIds").get("DHT11").isNumber()).isTrue();
        assertThat(body.get("sensorIds").get("GUVA_S12SD").isNumber()).isTrue();
    }

    @Test
    void provision_sameSerialTwice_returnsSameNodeId() {
        String serial = SERIAL + "-IDEM";

        int nodeIdFirst  = provision(serial, "BMP280", "DHT11", "GUVA_S12SD").get("nodeId").asInt();
        int nodeIdSecond = provision(serial, "BMP280", "DHT11", "GUVA_S12SD").get("nodeId").asInt();

        assertThat(nodeIdSecond).isEqualTo(nodeIdFirst);
    }

    @Test
    void provision_missingApiKey_returns401() {
        var resp = rest.postForEntity("/api/gateway/nodes/register",
                json("""
                        {"serialNumber":"%s","sensors":["BMP280"]}
                        """.formatted(SERIAL + "-NOKEY")),
                String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void provision_wrongApiKey_returns401() {
        var resp = rest.postForEntity("/api/gateway/nodes/register",
                jsonWithApiKey("""
                        {"serialNumber":"%s","sensors":["BMP280"]}
                        """.formatted(SERIAL + "-BADKEY"), "not-a-real-api-key"),
                String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
