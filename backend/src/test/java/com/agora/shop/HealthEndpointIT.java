package com.agora.shop;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HealthEndpointIT {

    @LocalServerPort
    private int port;

    @Test
    void reportsUpWithoutExposingDetails() throws IOException, InterruptedException {
        var response = get("/actuator/health");

        assertThat(response.statusCode()).isEqualTo(200);
        var contentType = MediaType.parseMediaType(response.headers().firstValue("Content-Type").orElseThrow());
        assertThat(contentType.isCompatibleWith(MediaType.APPLICATION_JSON)
                || contentType.getSubtype().endsWith("+json")).isTrue();

        var mapper = JsonMapper.builder().build();
        assertThat(mapper.readTree(response.body())).isEqualTo(mapper.readTree("""
                {"status":"UP"}
                """));
    }

    @Test
    void doesNotExposeEnvironment() throws IOException, InterruptedException {
        assertThat(get("/actuator/env").statusCode()).isEqualTo(404);
    }

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        try (var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }
    }
}
