package com.example.meteonode;

import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.testcontainers.containers.MySQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class AbstractIntegrationTest {

    protected static final String API_KEY = "esp32-test-key-001";

    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("meteonode");

    static {
        mysql.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @LocalServerPort
    int port;

    RestTemplate rest;

    @BeforeEach
    void initRest() {
        rest = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
        rest.setErrorHandler(new ResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) throws IOException { return false; }
            @Override
            public void handleError(URI url, HttpMethod method, ClientHttpResponse response) throws IOException {}
        });
        rest.setUriTemplateHandler(new DefaultUriBuilderFactory("http://localhost:" + port));
    }

    protected JsonNode provision(String serial, String... sensors) {
        String sensorList = String.join("\",\"", sensors);
        var resp = rest.postForEntity("/api/gateway/nodes/register",
                jsonWithApiKey("""
                        {"serialNumber":"%s","sensors":["%s"]}
                        """.formatted(serial, sensorList), API_KEY),
                JsonNode.class);
        assertThat(resp.getStatusCode().value()).isIn(200, 201);
        return resp.getBody();
    }

    protected HttpEntity<String> json(String body) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, h);
    }

    protected HttpEntity<String> jsonWithApiKey(String body, String apiKey) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("X-Api-Key", apiKey);
        return new HttpEntity<>(body, h);
    }

    protected HttpEntity<Void> bearer(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        return new HttpEntity<>(h);
    }

    protected HttpEntity<String> jsonWithBearer(String body, String token) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(token);
        return new HttpEntity<>(body, h);
    }

    protected boolean containsId(JsonNode array, int id) {
        for (JsonNode node : array) {
            if (node.get("id").asInt() == id) return true;
        }
        return false;
    }

    protected String loginAs(String username, String password) {
        var resp = rest.postForEntity("/api/auth/login",
                json("""
                        {"username":"%s","password":"%s"}
                        """.formatted(username, password)),
                JsonNode.class);
        return resp.getBody().get("accessToken").asText();
    }
}
