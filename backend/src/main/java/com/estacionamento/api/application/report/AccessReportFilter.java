package com.estacionamento.api.application.report;

import com.estacionamento.api.domain.movement.AccessDecision;
import com.estacionamento.api.domain.movement.MovementDirection;
import com.estacionamento.api.domain.movement.MovementSource;
import com.estacionamento.api.domain.movement.MovementStatus;

import java.time.OffsetDateTime;

public record AccessReportFilter(
        OffsetDateTime from,
        OffsetDateTime to,
        String plate,
        MovementDirection direction,
        MovementStatus status,
        AccessDecision decision,
        MovementSource source,
        Long cameraId,
        Long apartmentId
) {
}
