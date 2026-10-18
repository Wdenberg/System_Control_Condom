package com.estacionamento.api.web.apartment;

import com.estacionamento.api.domain.apartment.Apartment;

public record ApartmentResponse(Long id, String number, boolean active) {
    public static ApartmentResponse from(Apartment apartment) {
        return new ApartmentResponse(apartment.getId(), apartment.getNumber(), apartment.isActive());
    }
}
