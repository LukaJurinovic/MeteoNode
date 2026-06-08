package com.example.meteonode;

import com.example.meteonode.service.domain.NodeCommandService;
import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class NodeCommandIntegrationTest extends AbstractIntegrationTest {

    private static final String SERIAL = "NODE-CMD-TEST-001";

    @Autowired
    NodeCommandService nodeCommandService;

    private JsonNode issueCommand(int nodeId, String command, String token) {
        var resp = rest.exchange("/api/nodes/%d/commands".formatted(nodeId), HttpMethod.POST,
                jsonWithBearer("""
                        {"command":"%s"}
                        """.formatted(command), token),
                JsonNode.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody();
    }

    @Test
    void issueCommand_createsPendingCommand() {
        int nodeId = provision(SERIAL + "-ISSUE", "BMP280").get("nodeId").asInt();
        String token = loginAs("admin", "admin123");

        JsonNode cmd = issueCommand(nodeId, "REBOOT", token);
        assertThat(cmd.get("status").asText()).isEqualTo("PENDING");
        assertThat(cmd.get("command").asText()).isEqualTo("REBOOT");
    }

    @Test
    void pollPendingCommands_returnsIssuedCommand() {
        String serial = SERIAL + "-POLL";
        int nodeId = provision(serial, "BMP280").get("nodeId").asInt();
        String token = loginAs("admin", "admin123");

        issueCommand(nodeId, "REQUEST_READINGS", token);

        var poll = rest.exchange("/api/gateway/nodes/%s/commands/pending".formatted(serial),
                HttpMethod.GET, jsonWithApiKey(null, API_KEY), JsonNode.class);
        assertThat(poll.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(poll.getBody().isArray()).isTrue();
        assertThat(poll.getBody().size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void confirmCommand_marksDelivered_removesFromPendingPoll() {
        String serial = SERIAL + "-CONFIRM";
        int nodeId = provision(serial, "BMP280").get("nodeId").asInt();
        String token = loginAs("admin", "admin123");

        JsonNode cmd = issueCommand(nodeId, "REBOOT", token);
        int cmdId = cmd.get("id").asInt();

        var confirm = rest.postForEntity("/api/gateway/nodes/%s/commands/%d/confirm".formatted(serial, cmdId),
                jsonWithApiKey("{}", API_KEY), Void.class);
        assertThat(confirm.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        var poll = rest.exchange("/api/gateway/nodes/%s/commands/pending".formatted(serial),
                HttpMethod.GET, jsonWithApiKey(null, API_KEY), JsonNode.class);
        for (JsonNode c : poll.getBody()) {
            assertThat(c.get("id").asInt()).isNotEqualTo(cmdId);
        }
    }

    @Test
    void pollPendingCommands_leased_notReturnedAgainWithinResendWindow() {
        String serial = SERIAL + "-LEASE";
        int nodeId = provision(serial, "BMP280").get("nodeId").asInt();
        String token = loginAs("admin", "admin123");

        int cmdId = issueCommand(nodeId, "REQUEST_READINGS", token).get("id").asInt();

        var first = rest.exchange("/api/gateway/nodes/%s/commands/pending".formatted(serial),
                HttpMethod.GET, jsonWithApiKey(null, API_KEY), JsonNode.class);
        assertThat(containsId(first.getBody(), cmdId)).isTrue();

        var second = rest.exchange("/api/gateway/nodes/%s/commands/pending".formatted(serial),
                HttpMethod.GET, jsonWithApiKey(null, API_KEY), JsonNode.class);
        assertThat(containsId(second.getBody(), cmdId)).isFalse();
    }

    @Test
    void confirmCommand_twice_isIdempotent_returns204() {
        String serial = SERIAL + "-IDEMPOTENT";
        int nodeId = provision(serial, "BMP280").get("nodeId").asInt();
        String token = loginAs("admin", "admin123");

        int cmdId = issueCommand(nodeId, "REBOOT", token).get("id").asInt();

        var first = rest.postForEntity("/api/gateway/nodes/%s/commands/%d/confirm".formatted(serial, cmdId),
                jsonWithApiKey("{}", API_KEY), Void.class);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        var second = rest.postForEntity("/api/gateway/nodes/%s/commands/%d/confirm".formatted(serial, cmdId),
                jsonWithApiKey("{}", API_KEY), Void.class);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void userRole_cannotIssueCommand_returns403() {
        int nodeId = provision(SERIAL + "-ROLE", "BMP280").get("nodeId").asInt();
        String viewerToken = loginAs("viewer", "admin123");

        var resp = rest.exchange("/api/nodes/%d/commands".formatted(nodeId), HttpMethod.POST,
                jsonWithBearer("""
                        {"command":"REBOOT"}
                        """, viewerToken),
                String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void expireStaleCommands_expiresPendingCommand() {
        int nodeId = provision(SERIAL + "-EXPIRE", "BMP280").get("nodeId").asInt();
        String token = loginAs("admin", "admin123");

        issueCommand(nodeId, "REQUEST_READINGS", token);

        int expired = nodeCommandService.expireStaleCommands(Instant.now().plus(1, ChronoUnit.HOURS));
        assertThat(expired).isGreaterThanOrEqualTo(1);
    }
}
