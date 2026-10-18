package com.estacionamento.api.web.movement;

import com.estacionamento.api.domain.movement.MovementDirection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ManualMovementRequest(
        @NotBlank String plate,
        @NotNull MovementDirection direction,
        @NotBlank @Size(min = 5, max = 1000) String justification
) {
}
