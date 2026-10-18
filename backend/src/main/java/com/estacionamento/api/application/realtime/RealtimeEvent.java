package com.estacionamento.api.application.realtime;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RealtimeEvent(
        String id,
        String type,
        OffsetDateTime occurredAt,
        Object payload
) {
    public RealtimeEvent(String type, Object payload, OffsetDateTime occurredAt) {
        this(UUID.randomUUID().toString(), type, occurredAt, payload);
    }
}
