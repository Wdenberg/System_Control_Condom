package com.estacionamento.api.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.hardening")
public record SecurityHardeningProperties(
        String corsAllowedOrigins,
        boolean trustForwardedHeaders,
        boolean hstsEnabled,
        boolean rateLimitEnabled,
        int loginRequestsPerMinute,
        int hikvisionRequestsPerMinute,
        boolean rateLimitFailOpen
) {
    public SecurityHardeningProperties {
        corsAllowedOrigins = corsAllowedOrigins == null ? "http://localhost:5173" : corsAllowedOrigins;
        loginRequestsPerMinute = normalize(loginRequestsPerMinute, 10);
        hikvisionRequestsPerMinute = normalize(hikvisionRequestsPerMinute, 120);
    }

    private static int normalize(int value, int fallback) {
        return value < 1 ? fallback : value;
    }
}
