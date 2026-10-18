package com.estacionamento.api.config.properties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.HashSet;
import java.util.Map;
@ConfigurationProperties(prefix = "app.hikvision.webhook")
public record WebhookCredentials(Map<String, String> tokens) {
    public WebhookCredentials {
        tokens = tokens == null ? Map.of() : tokens.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(entry -> entry.getKey().toUpperCase(java.util.Locale.ROOT), Map.Entry::getValue));
        var distinct = new HashSet<String>();
        for (String token : tokens.values()) {
            if (!token.isBlank() && !distinct.add(token)) throw new IllegalArgumentException("Webhook credentials must differ per camera");
            if (!token.isBlank() && (token.length() < 32 || token.toUpperCase(java.util.Locale.ROOT).startsWith("CHANGE_ME"))) throw new IllegalArgumentException("Webhook credentials require at least 32 characters");
        }
    }
}
