package com.estacionamento.api.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.gate")
public record GateProperties(
        boolean enabled,
        String driver,
        String entryUrl,
        String exitUrl,
        String username,
        String password,
        String bearerToken,
        Duration timeout,
        int maxAttempts,
        Duration retryDelay,
        boolean includePlateInCommand,
        int openPulseMillis
) {
    public GateProperties {
        driver = driver == null || driver.isBlank() ? "SIMULATED" : driver;
        timeout = timeout == null ? Duration.ofSeconds(3) : timeout;
        retryDelay = retryDelay == null ? Duration.ofSeconds(5) : retryDelay;
        maxAttempts = maxAttempts < 1 ? 3 : maxAttempts;
        openPulseMillis = openPulseMillis < 1 ? 1000 : openPulseMillis;
    }
}
