package com.estacionamento.api.application.gate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GateTestRequest(
        @NotBlank
        @Size(min = 5, max = 500)
        String justification
) {
}
