package com.estacionamento.api.infrastructure.gate;

import com.estacionamento.api.domain.movement.MovementDirection;

public record GateOpenCommand(
        String operationKey,
        MovementDirection direction,
        String plate,
        int pulseMillis
) {
}
