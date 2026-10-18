package com.estacionamento.api.web.registration;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import com.estacionamento.api.domain.parking.ParkingSpaceType;
import java.util.*;
public record UnifiedRegistrationRequest(
    @NotNull UUID requestId,
    @NotBlank @Size(max=20) String apartmentNumber, boolean apartmentActive,
    @NotEmpty @Size(max=20) List<@Valid ResidentInput> residents,
    @Size(max=20) String parkingNumber, ParkingSpaceType parkingType, boolean parkingActive,
    @Size(max=20) String plate, @Size(max=80) String brand, @Size(max=80) String model,
    @Size(max=40) String color, boolean vehicleActive
) {
    public record ResidentInput(@NotBlank @Size(min=2,max=160) String fullName, boolean active) {}
}
