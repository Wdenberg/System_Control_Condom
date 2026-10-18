package com.estacionamento.api.web.vehicle;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record VehicleRequest(
        @NotBlank @Size(max = 20) String plate,
        @Size(max = 80) String brand,
        @Size(max = 80) String model,
        @Size(max = 40) String color,
        @NotEmpty Set<Long> residentIds,
        Long parkingSpaceId,
        boolean active
) {
}
