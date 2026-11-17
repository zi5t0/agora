package com.agora.shop.diagnostics.internal.adapter.in.web;

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
class TestEndpointIT {

    @LocalServerPort
    private int port;

    @Test
    void returnsTestMessageAsJson() throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/test"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        try (var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode()).isEqualTo(200);
            var contentType = MediaType.parseMediaType(response.headers().firstValue("Content-Type").orElseThrow());
            assertThat(contentType.isCompatibleWith(MediaType.APPLICATION_JSON)).isTrue();

            var mapper = JsonMapper.builder().build();
            assertThat(mapper.readTree(response.body())).isEqualTo(mapper.readTree("""
                    {"message":"Esto es una prueba"}
                    """));
        }
    }
}
