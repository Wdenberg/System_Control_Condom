package com.estacionamento.api.application.report;

import com.estacionamento.api.domain.gate.GateOperation;

import java.time.OffsetDateTime;

public record GateReportRow(
        Long id,
        Long movementId,
        String operationKey,
        String plate,
        String direction,
        String command,
        String status,
        String driver,
        int attempts,
        String lastError,
        OffsetDateTime requestedAt,
        OffsetDateTime sentAt
) {
    public static GateReportRow from(GateOperation operation) {
        return new GateReportRow(
                operation.getId(),
                operation.getMovement().getId(),
                operation.getOperationKey(),
                operation.getMovement().getPlate(),
                operation.getMovement().getDirection().name(),
                operation.getCommand().name(),
                operation.getStatus().name(),
                operation.getDriver(),
                operation.getAttemptCount(),
                operation.getLastError(),
                operation.getRequestedAt(),
                operation.getSentAt()
        );
    }
}
