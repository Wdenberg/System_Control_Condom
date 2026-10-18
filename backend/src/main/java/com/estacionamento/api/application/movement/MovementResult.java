package com.estacionamento.api.application.movement;

import com.estacionamento.api.domain.camera.CameraDirection;
import com.estacionamento.api.domain.movement.AccessDecision;
import com.estacionamento.api.domain.movement.Movement;
import com.estacionamento.api.domain.movement.MovementStatus;

import java.time.OffsetDateTime;

public record MovementResult(
        Long movementId,
        String plate,
        CameraDirection direction,
        MovementStatus status,
        AccessDecision decision,
        Long vehicleId,
        String vehicleOwner,
        String apartmentNumber,
        Long parkingSpaceId,
        String parkingSpaceNumber,
        OffsetDateTime eventTime,
        com.estacionamento.api.domain.movement.PassageState passageState
) {
    public static MovementResult from(Movement movement) {
        String owner = movement.getVehicle() == null ? null : movement.getVehicle().getResidents().stream()
                .filter(r -> r.isActive())
                .map(r -> r.getFullName())
                .findFirst()
                .orElse(null);
        String apartment = movement.getVehicle() == null || movement.getVehicle().getResidents().isEmpty()
                ? null
                : movement.getVehicle().getResidents().stream()
                .filter(r -> r.getApartment() != null)
                .map(r -> r.getApartment().getNumber())
                .findFirst()
                .orElse(null);
        return new MovementResult(
                movement.getId(),
                movement.getPlate(),
                movement.getCamera() == null ? (movement.getDirection() == com.estacionamento.api.domain.movement.MovementDirection.ENTRY ? CameraDirection.ENTRY : CameraDirection.EXIT) : movement.getCamera().getDirection(),
                movement.getStatus(),
                movement.getAccessDecision(),
                movement.getVehicle() == null ? null : movement.getVehicle().getId(),
                owner,
                apartment,
                movement.getParkingSpace() == null ? null : movement.getParkingSpace().getId(),
                movement.getParkingSpace() == null ? null : movement.getParkingSpace().getNumber(),
                movement.getEventTime(),
                movement.getPassageState()
        );
    }
}
