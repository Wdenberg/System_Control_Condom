package com.estacionamento.api.web.movement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OverrideMovementRequest(
        @NotBlank @Size(min = 5, max = 1000) String justification
) {
}
