package com.estacionamento.api.infrastructure.gate;

import com.estacionamento.api.domain.movement.MovementDirection;

public interface GateDriver {
    GateCommandResult open(GateOpenCommand command);

    default GateDriverStatus status(MovementDirection direction) {
        return GateDriverStatus.configured("Driver loaded; hardware connectivity is not probed automatically");
    }
}
