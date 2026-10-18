package com.estacionamento.api.application.report;

import com.estacionamento.api.application.movement.MovementResult;
import com.estacionamento.api.domain.movement.Movement;

import java.time.OffsetDateTime;

public record AccessReportRow(
        Long movementId,
        OffsetDateTime eventTime,
        String plate,
        String direction,
        String source,
        String status,
        String decision,
        Integer confidenceLevel,
        String vehicleOwner,
        String apartmentNumber,
        String parkingSpaceNumber,
        String cameraName,
        String cameraIp
) {
    public static AccessReportRow from(Movement movement) {
        MovementResult result = MovementResult.from(movement);
        return new AccessReportRow(
                movement.getId(),
                movement.getEventTime(),
                movement.getPlate(),
                movement.getDirection().name(),
                movement.getSource().name(),
                movement.getStatus().name(),
                movement.getAccessDecision().name(),
                movement.getConfidenceLevel(),
                result.vehicleOwner(),
                result.apartmentNumber(),
                result.parkingSpaceNumber(),
                movement.getCamera() == null ? null : movement.getCamera().getName(),
                movement.getCamera() == null ? null : movement.getCamera().getIpAddress()
        );
    }
}
