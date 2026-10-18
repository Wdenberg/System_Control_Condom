package com.estacionamento.api.infrastructure.gate;

import com.estacionamento.api.domain.movement.MovementDirection;

public record GateDriverStatus(
        GateDriverState state,
        String message
) {
    public static GateDriverStatus ready(String message) {
        return new GateDriverStatus(GateDriverState.READY, message);
    }

    public static GateDriverStatus configured(String message) {
        return new GateDriverStatus(GateDriverState.CONFIGURED, message);
    }

    public static GateDriverStatus notConfigured(String message) {
        return new GateDriverStatus(GateDriverState.NOT_CONFIGURED, message);
    }

    public static GateDriverStatus disabled(String message) {
        return new GateDriverStatus(GateDriverState.DISABLED, message);
    }

    public static GateDriverStatus error(String message) {
        return new GateDriverStatus(GateDriverState.ERROR, message);
    }
}
