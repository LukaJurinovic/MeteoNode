package com.example.meteonode;

import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class UserManagementIntegrationTest extends AbstractIntegrationTest {

    private void register(String username, String email) {
        rest.postForEntity("/api/auth/register",
                json("""
                        {"username":"%s","email":"%s","password":"testpass123"}
                        """.formatted(username, email)),
                Void.class);
    }

    @Test
    void adminCanListAllUsers() {
        String token = loginAs("admin", "admin123");
        var resp = rest.exchange("/api/users", HttpMethod.GET, bearer(token), JsonNode.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().isArray()).isTrue();
        assertThat(resp.getBody().size()).isGreaterThan(0);
    }

    @Test
    void nonAdmin_cannotListAllUsers_returns403() {
        String token = loginAs("viewer", "admin123");
        var resp = rest.exchange("/api/users", HttpMethod.GET, bearer(token), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void adminCanChangeUserRole() {
        register("role-change-user", "role-change@test.com");
        String token = loginAs("admin", "admin123");

        var resp = rest.exchange("/api/users/role", HttpMethod.PATCH,
                jsonWithBearer("""
                        {"username":"role-change-user","role":"OPERATOR"}
                        """, token),
                Void.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        var users = rest.exchange("/api/users", HttpMethod.GET, bearer(token), JsonNode.class);
        String roleFound = null;
        for (JsonNode u : users.getBody()) {
            if ("role-change-user".equals(u.get("username").asText())) {
                roleFound = u.get("role").asText();
                break;
            }
        }
        assertThat(roleFound).isEqualTo("OPERATOR");
    }

    @Test
    void adminCanDeleteUser_thenNotFound() {
        register("delete-me-user", "delete-me@test.com");
        String token = loginAs("admin", "admin123");

        var del = rest.exchange("/api/users/delete-me-user", HttpMethod.DELETE, bearer(token), Void.class);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        var get = rest.exchange("/api/users/delete-me-user", HttpMethod.DELETE, bearer(token), String.class);
        assertThat(get.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void nonAdmin_cannotDeleteUser_returns403() {
        register("protected-user", "protected@test.com");
        String token = loginAs("viewer", "admin123");

        var resp = rest.exchange("/api/users/protected-user", HttpMethod.DELETE, bearer(token), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
