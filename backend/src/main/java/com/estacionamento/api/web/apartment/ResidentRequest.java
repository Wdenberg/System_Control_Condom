package com.estacionamento.api.web.apartment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResidentRequest(
        @NotBlank @Size(max = 160) String fullName,
        boolean active
) {
}
