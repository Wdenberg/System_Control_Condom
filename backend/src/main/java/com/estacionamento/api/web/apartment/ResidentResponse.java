package com.estacionamento.api.web.apartment;

import com.estacionamento.api.domain.resident.Resident;

public record ResidentResponse(Long id, String fullName, Long apartmentId, String apartmentNumber, boolean active) {
    public static ResidentResponse from(Resident resident) {
        return new ResidentResponse(
                resident.getId(), resident.getFullName(), resident.getApartment().getId(),
                resident.getApartment().getNumber(), resident.isActive()
        );
    }
}
