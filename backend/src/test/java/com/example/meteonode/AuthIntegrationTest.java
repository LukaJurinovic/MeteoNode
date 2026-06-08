package com.example.meteonode;

import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class AuthIntegrationTest extends AbstractIntegrationTest {

    @Test
    void register_login_and_access() {
        var registerResp = rest.postForEntity("/api/auth/register",
                json("""
                        {"username":"viewer_itest","email":"viewer_itest@test.com","password":"password123"}
                        """),
                Void.class);
        assertThat(registerResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        var loginResp = rest.postForEntity("/api/auth/login",
                json("""
                        {"username":"viewer_itest","password":"password123"}
                        """),
                JsonNode.class);
        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        String token = loginResp.getBody().get("accessToken").asText();
        assertThat(token).isNotBlank();

        var authedResp = rest.exchange("/api/stations", HttpMethod.GET, bearer(token), String.class);
        assertThat(authedResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        var noTokenResp = rest.getForEntity("/api/stations", String.class);
        assertThat(noTokenResp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        var forbiddenResp = rest.exchange("/api/stations", HttpMethod.POST,
                jsonWithBearer("""
                        {"name":"Forbidden Station","apiKey":"some-key"}
                        """, token),
                String.class);
        assertThat(forbiddenResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void login_withWrongPassword_returns401() {
        var resp = rest.postForEntity("/api/auth/login",
                json("""
                        {"username":"admin","password":"wrongpassword"}
                        """),
                String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void register_duplicateUsername_returns409() {
        rest.postForEntity("/api/auth/register",
                json("""
                        {"username":"dup_user","email":"dup1@test.com","password":"password123"}
                        """),
                Void.class);

        var resp = rest.postForEntity("/api/auth/register",
                json("""
                        {"username":"dup_user","email":"dup2@test.com","password":"password123"}
                        """),
                Void.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void refresh_returnsNewAccessToken() {
        rest.postForEntity("/api/auth/register",
                json("""
                        {"username":"refresh_user","email":"refresh_user@test.com","password":"password123"}
                        """),
                Void.class);

        var loginResp = rest.postForEntity("/api/auth/login",
                json("""
                        {"username":"refresh_user","password":"password123"}
                        """),
                JsonNode.class);
        String refreshToken = loginResp.getBody().get("refreshToken").asText();
        assertThat(refreshToken).isNotBlank();

        var refreshResp = rest.postForEntity("/api/auth/refresh",
                json("""
                        {"refreshToken":"%s"}
                        """.formatted(refreshToken)),
                JsonNode.class);
        assertThat(refreshResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refreshResp.getBody().get("accessToken").asText()).isNotBlank();
        assertThat(refreshResp.getBody().get("refreshToken").asText()).isNotBlank();
    }
}
