package com.estacionamento.api.infrastructure.gate;

import com.estacionamento.api.domain.movement.MovementDirection;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SimulatedGateDriverTest {

    @Test
    void shouldOpenSimulatedGate() {
        SimulatedGateDriver driver = new SimulatedGateDriver();

        GateCommandResult result = driver.open(new GateOpenCommand(
                "op-1", MovementDirection.ENTRY, "ABC1D23", 1000));

        assertThat(result.success()).isTrue();
    }
}
