package com.estacionamento.api.web.parking;

import com.estacionamento.api.domain.parking.ParkingSpace;
import com.estacionamento.api.domain.parking.ParkingSpaceStatus;
import com.estacionamento.api.domain.parking.ParkingSpaceType;

public record ParkingSpaceResponse(
        Long id,
        String number,
        Long apartmentId,
        String apartmentNumber,
        ParkingSpaceType type,
        ParkingSpaceStatus status,
        boolean active
) {
    public static ParkingSpaceResponse from(ParkingSpace space) {
        return new ParkingSpaceResponse(
                space.getId(), space.getNumber(),
                space.getApartment() == null ? null : space.getApartment().getId(),
                space.getApartment() == null ? null : space.getApartment().getNumber(),
                space.getType(), space.getStatus(), space.isActive()
        );
    }
}
