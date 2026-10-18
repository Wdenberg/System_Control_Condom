package com.estacionamento.api.web.apartment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApartmentRequest(
        @NotBlank @Size(max = 20) String number,
        boolean active
) {
}
