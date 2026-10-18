package com.estacionamento.api.application.gate;

import com.estacionamento.api.domain.gate.GateOperation;
import com.estacionamento.api.domain.gate.GateOperationStatus;
import com.estacionamento.api.domain.movement.MovementDirection;

import java.time.OffsetDateTime;

public record GateOperationResponse(
        Long id,
        String operationKey,
        Long movementId,
        String plate,
        MovementDirection direction,
        String command,
        GateOperationStatus status,
        String driver,
        int attempts,
        String lastError,
        OffsetDateTime requestedAt,
        OffsetDateTime sentAt
) {
    public static GateOperationResponse from(GateOperation operation) {
        return new GateOperationResponse(
                operation.getId(),
                operation.getOperationKey(),
                operation.getMovement().getId(),
                operation.getMovement().getPlate(),
                operation.getMovement().getDirection(),
                operation.getCommand().name(),
                operation.getStatus(),
                operation.getDriver(),
                operation.getAttemptCount(),
                operation.getLastError(),
                operation.getRequestedAt(),
                operation.getSentAt()
        );
    }
}
