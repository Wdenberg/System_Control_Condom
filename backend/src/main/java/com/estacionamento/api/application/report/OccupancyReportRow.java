package com.estacionamento.api.application.report;

public record OccupancyReportRow(
        Long parkingSpaceId,
        String parkingSpaceNumber,
        Long apartmentId,
        String apartmentNumber,
        String type,
        String status,
        boolean active,
        String assignedPlate,
        boolean vehicleInside
) {
}
