package com.estacionamento.api.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.time.Duration;

@ConfigurationProperties(prefix = "app.hikvision")
public record HikvisionProperties(
        String entryHost,
        String exitHost,
        int port,
        String username,
        String password,
        String webhookToken,
        Duration cooldown,
        Path imageStoragePath,
        boolean enforceRemoteIp
) {
    public HikvisionProperties {
        entryHost = entryHost == null ? "" : entryHost.trim();
        exitHost = exitHost == null ? "" : exitHost.trim();
        port = port < 1 ? 80 : port;
        username = username == null ? "" : username;
        password = password == null ? "" : password;
        cooldown = cooldown == null ? Duration.ofSeconds(30) : cooldown;
        if (cooldown.isNegative() || cooldown.isZero()) {
            cooldown = Duration.ofSeconds(30);
        }
        imageStoragePath = imageStoragePath == null ? Path.of("data/hikvision") : imageStoragePath;
    }
}
