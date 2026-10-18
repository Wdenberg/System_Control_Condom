package com.estacionamento.api.infrastructure.gate;

import com.estacionamento.api.config.properties.GateProperties;
import com.estacionamento.api.domain.movement.MovementDirection;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Component("HTTP")
public class HttpGateDriver implements GateDriver {

    private final GateProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @org.springframework.beans.factory.annotation.Autowired
    public HttpGateDriver(GateProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, HttpClient.newBuilder().connectTimeout(properties.timeout()).build());
    }

    public HttpGateDriver(GateProperties properties, ObjectMapper objectMapper, HttpClient httpClient) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public GateCommandResult open(GateOpenCommand command) {
        String url = command.direction() == com.estacionamento.api.domain.movement.MovementDirection.ENTRY
                ? properties.entryUrl() : properties.exitUrl();
        if (url == null || url.isBlank()) {
            return GateCommandResult.failure("No gate HTTP URL configured for " + command.direction());
        }

        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("operationId", command.operationKey());
            body.put("command", "OPEN");
            body.put("direction", command.direction().name());
            body.put("pulseMillis", command.pulseMillis());
            if (properties.includePlateInCommand()) {
                body.put("plate", command.plate());
            }

            String json = objectMapper.writeValueAsString(body);
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(properties.timeout())
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("X-Gate-Operation-Id", command.operationKey())
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));

            if (properties.bearerToken() != null && !properties.bearerToken().isBlank()) {
                requestBuilder.header("Authorization", "Bearer " + properties.bearerToken());
            } else if (properties.username() != null && !properties.username().isBlank()) {
                String basic = properties.username() + ":" + (properties.password() == null ? "" : properties.password());
                requestBuilder.header("Authorization", "Basic " + Base64.getEncoder().encodeToString(basic.getBytes(StandardCharsets.UTF_8)));
            }

            HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                return GateCommandResult.success("HTTP gate accepted command (" + status + ")");
            }
            return java.util.Set.of(400, 401, 403, 404, 422).contains(status)
                    ? GateCommandResult.failure("HTTP gate rejected command (" + status + ")")
                    : GateCommandResult.unknown("Controller returned an uncertain HTTP result (" + status + ")");
        } catch (java.net.ConnectException exception) {
            return GateCommandResult.failure("Controller connection was not established");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return GateCommandResult.unknown("Controller request interrupted; physical outcome is unknown");
        } catch (Exception exception) {
            return GateCommandResult.unknown("Controller communication failed; physical outcome is unknown");
        }
    }


    @Override
    public GateDriverStatus status(MovementDirection direction) {
        String url = direction == MovementDirection.ENTRY ? properties.entryUrl() : properties.exitUrl();
        if (url == null || url.isBlank()) {
            return GateDriverStatus.notConfigured("No HTTP gate URL configured for " + direction);
        }
        return GateDriverStatus.configured("HTTP endpoint configured for " + direction + "; connectivity is verified only when an OPEN command is sent");
    }

    private String truncate(String value) {
        if (value == null) return "";
        return value.length() <= 500 ? value : value.substring(0, 500);
    }
}
