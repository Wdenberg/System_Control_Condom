package com.estacionamento.api.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.initial-admin")
public record InitialAdminProperties(
        boolean enabled,
        String username,
        String password,
        String fullName,
        String role
) {
}
