package com.estacionamento.api.application.audit;

import java.time.OffsetDateTime;

public record AuditFilter(
        OffsetDateTime from,
        OffsetDateTime to,
        String username,
        String action,
        String entityType,
        String entityId,
        String ipAddress,
        String correlationId
) {
}
