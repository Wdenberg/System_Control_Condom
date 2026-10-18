package com.estacionamento.api.application.hikvision;

import com.estacionamento.api.domain.camera.CameraDirection;
import com.estacionamento.api.domain.hikvision.HikvisionProcessingStatus;
import com.estacionamento.api.domain.movement.AccessDecision;

import java.time.OffsetDateTime;

public record HikvisionEventResponse(
        Long eventId,
        String status,
        String externalEventId,
        String cameraName,
        CameraDirection cameraDirection,
        String plate,
        Integer confidence,
        OffsetDateTime occurredAt,
        int pictureCount,
        Long movementId,
        String movementStatus,
        AccessDecision accessDecision
) {
    public static HikvisionEventResponse of(Long eventId, HikvisionProcessingStatus status,
                                             String externalEventId, String cameraName,
                                             CameraDirection cameraDirection, String plate,
                                             Integer confidence, OffsetDateTime occurredAt, int pictureCount) {
        return new HikvisionEventResponse(
                eventId,
                status.name(),
                externalEventId,
                cameraName,
                cameraDirection,
                plate,
                confidence,
                occurredAt,
                pictureCount,
                null,
                null,
                null
        );
    }
    public static HikvisionEventResponse of(Long eventId, HikvisionProcessingStatus status,
                                             String externalEventId, String cameraName,
                                             CameraDirection cameraDirection, String plate,
                                             Integer confidence, OffsetDateTime occurredAt, int pictureCount,
                                             com.estacionamento.api.application.movement.MovementResult movement) {
        return new HikvisionEventResponse(
                eventId, status.name(), externalEventId, cameraName, cameraDirection, plate,
                confidence, occurredAt, pictureCount,
                movement == null ? null : movement.movementId(),
                movement == null ? null : movement.status().name(),
                movement == null ? null : movement.decision()
        );
    }

}

