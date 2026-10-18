package com.estacionamento.api.web.parking;

import com.estacionamento.api.domain.parking.ParkingSpaceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ParkingSpaceRequest(
        @NotBlank @Size(max = 20) String number,
        Long apartmentId,
        ParkingSpaceType type,
        boolean active
) {
}
