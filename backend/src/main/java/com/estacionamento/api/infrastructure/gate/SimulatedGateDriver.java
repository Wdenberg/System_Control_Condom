package com.estacionamento.api.infrastructure.gate;

import com.estacionamento.api.domain.movement.MovementDirection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("SIMULATED")
public class SimulatedGateDriver implements GateDriver {

    private static final Logger log = LoggerFactory.getLogger(SimulatedGateDriver.class);

    @Override
    public GateCommandResult open(GateOpenCommand command) {
        log.info("SIMULATED GATE OPEN operation={} direction={} plate={} pulseMs={}",
                command.operationKey(), command.direction(), command.plate(), command.pulseMillis());
        return GateCommandResult.success("Simulated gate opened");
    }

    @Override
    public GateDriverStatus status(MovementDirection direction) {
        return GateDriverStatus.ready("Simulation driver is ready; no physical gate is connected");
    }
}
