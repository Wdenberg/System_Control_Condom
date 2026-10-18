package com.estacionamento.api.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.dashboard")
public record DashboardProperties(
        String zoneId,
        Duration cacheTtl
) {
    public DashboardProperties {
        zoneId = zoneId == null || zoneId.isBlank() ? "America/Recife" : zoneId;
        cacheTtl = cacheTtl == null ? Duration.ofSeconds(5) : cacheTtl;
        if (cacheTtl.isNegative() || cacheTtl.isZero()) {
            cacheTtl = Duration.ofSeconds(5);
        }
    }
}
