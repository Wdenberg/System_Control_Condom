package com.estacionamento.api.application.dashboard;

import com.estacionamento.api.domain.parking.ParkingSpaceStatus;
import com.estacionamento.api.domain.parking.ParkingSpaceType;

public record DashboardOccupancyResponse(
        Long parkingSpaceId,
        String parkingSpaceNumber,
        Long apartmentId,
        String apartmentNumber,
        ParkingSpaceType type,
        ParkingSpaceStatus status,
        boolean active,
        String assignedPlate,
        boolean vehicleInside
) {
}
