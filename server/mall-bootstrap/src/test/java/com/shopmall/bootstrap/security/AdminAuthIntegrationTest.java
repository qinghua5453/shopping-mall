package com.shopmall.bootstrap.security;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.bootstrap-admin.enabled=true",
                "app.bootstrap-admin.username=admin",
                "app.bootstrap-admin.password=Admin@123456",
                "app.auth.jwt-secret=integration-test-jwt-secret-with-at-least-32-bytes"
        })
@Testcontainers(disabledWithoutDocker = true)
class AdminAuthIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7").withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", REDIS::getFirstMappedPort);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void loginRefreshMeAndLogout() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Client-Type", "PC");
        Map<String, String> login = Map.of(
                "username", "admin",
                "password", "Admin@123456",
                "deviceId", "integration-device-001");

        ResponseEntity<JsonNode> loginResponse = restTemplate.postForEntity(
                "/api/v1/admin/auth/login", new HttpEntity<>(login, headers), JsonNode.class);

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResponse.getBody()).isNotNull();
        assertThat(loginResponse.getBody().path("code").asInt()).isZero();
        String accessToken = loginResponse.getBody().path("data").path("accessToken").asText();
        String refreshToken = loginResponse.getBody().path("data").path("refreshToken").asText();
        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).startsWith("r_");

        headers.setBearerAuth(accessToken);
        ResponseEntity<JsonNode> meResponse = restTemplate.exchange(
                "/api/v1/admin/auth/me", HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
        assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(meResponse.getBody()).isNotNull();
        assertThat(meResponse.getBody().path("data").path("admin").path("username").asText())
                .isEqualTo("admin");
        assertThat(meResponse.getBody().path("data").path("permissions").get(0).asText())
                .isEqualTo("*");

        headers = new HttpHeaders();
        headers.set("X-Client-Type", "PC");
        Map<String, String> refresh = Map.of(
                "refreshToken", refreshToken,
                "deviceId", "integration-device-001");
        ResponseEntity<JsonNode> refreshResponse = restTemplate.postForEntity(
                "/api/v1/admin/auth/refresh", new HttpEntity<>(refresh, headers), JsonNode.class);
        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refreshResponse.getBody()).isNotNull();
        String rotatedAccessToken = refreshResponse.getBody().path("data").path("accessToken").asText();

        ResponseEntity<JsonNode> replayResponse = restTemplate.postForEntity(
                "/api/v1/admin/auth/refresh", new HttpEntity<>(refresh, headers), JsonNode.class);
        assertThat(replayResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(replayResponse.getBody()).isNotNull();
        assertThat(replayResponse.getBody().path("code").asInt()).isEqualTo(40108);

        headers.setBearerAuth(rotatedAccessToken);
        ResponseEntity<JsonNode> logoutResponse = restTemplate.postForEntity(
                "/api/v1/admin/auth/logout", new HttpEntity<>(headers), JsonNode.class);
        assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<JsonNode> afterLogout = restTemplate.exchange(
                "/api/v1/admin/auth/me", HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
        assertThat(afterLogout.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(afterLogout.getBody()).isNotNull();
        assertThat(afterLogout.getBody().path("code").asInt()).isEqualTo(40102);
    }
}
