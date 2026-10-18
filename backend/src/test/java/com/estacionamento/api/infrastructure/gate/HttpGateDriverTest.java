package com.estacionamento.api.infrastructure.gate;

import com.estacionamento.api.config.properties.GateProperties;
import com.estacionamento.api.domain.movement.MovementDirection;
import tools.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class HttpGateDriverTest {

    @Test
    void shouldSendOpenCommandToEntryUrl() throws Exception {
        AtomicReference<String> method = new AtomicReference<>();
        AtomicReference<String> operationId = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/entry", exchange -> {
            method.set(exchange.getRequestMethod());
            operationId.set(exchange.getRequestHeaders().getFirst("X-Gate-Operation-Id"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(response);
            }
        });
        server.start();
        try {
            GateProperties properties = new GateProperties(true, "HTTP",
                    "http://localhost:" + server.getAddress().getPort() + "/entry", "", "", "", "",
                    Duration.ofSeconds(2), 3, Duration.ofSeconds(1), true, 1000);
            HttpGateDriver driver = new HttpGateDriver(properties, new ObjectMapper());

            GateCommandResult result = driver.open(new GateOpenCommand(
                    "op-http", MovementDirection.ENTRY, "ABC1D23", 1000));

            assertThat(result.success()).isTrue();
            assertThat(method).hasValue("POST");
            assertThat(operationId).hasValue("op-http");
            assertThat(body.get()).contains("\"command\":\"OPEN\"");
            assertThat(body.get()).contains("\"direction\":\"ENTRY\"");
            assertThat(body.get()).contains("ABC1D23");
        } finally {
            server.stop(0);
        }
    }
}
