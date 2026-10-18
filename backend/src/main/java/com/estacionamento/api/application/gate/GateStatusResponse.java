package com.estacionamento.api.application.gate;

import com.estacionamento.api.infrastructure.gate.GateDriverState;

public record GateStatusResponse(
        boolean enabled,
        String driver,
        boolean entryConfigured,
        boolean exitConfigured,
        GateDriverState entryState,
        String entryMessage,
        GateDriverState exitState,
        String exitMessage,
        String safetyMode
) {
}
