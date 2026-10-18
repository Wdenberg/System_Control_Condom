package com.estacionamento.api.application.dashboard;

import java.time.OffsetDateTime;

public record DashboardSummaryResponse(
        long activeApartments,
        long activeResidents,
        long activeVehicles,
        long vehiclesInside,
        long vehiclesOutside,
        long totalParkingSpaces,
        long freeParkingSpaces,
        long occupiedParkingSpaces,
        long blockedParkingSpaces,
        long movementsToday,
        long entriesToday,
        long exitsToday,
        long authorizedToday,
        long deniedToday,
        long pendingGateOperations,
        long exhaustedGateOperations,
        OffsetDateTime generatedAt
) {
}
