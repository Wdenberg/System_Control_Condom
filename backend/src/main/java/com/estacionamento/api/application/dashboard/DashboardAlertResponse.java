package com.estacionamento.api.application.dashboard;

import java.time.OffsetDateTime;

public record DashboardAlertResponse(
        String id,
        String type,
        String severity,
        String title,
        String message,
        OffsetDateTime occurredAt,
        Long movementId,
        Long gateOperationId
) {
}
