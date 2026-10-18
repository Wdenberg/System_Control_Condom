package com.estacionamento.api.infrastructure.gate;

import com.estacionamento.api.config.properties.GateProperties;
import com.estacionamento.api.domain.movement.MovementDirection;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class GateDriverStatusTest {

    @Test
    void simulatedDriverIsReadyWithoutPhysicalHardware() {
        GateDriverStatus status = new SimulatedGateDriver().status(MovementDirection.ENTRY);

        assertThat(status.state()).isEqualTo(GateDriverState.READY);
    }

    @Test
    void httpDriverReportsMissingDirectionUrlWithoutPretendingConnectivity() {
        GateProperties properties = new GateProperties(true, "HTTP", "", "", "", "", "",
                Duration.ofSeconds(2), 3, Duration.ofSeconds(1), false, 1000);

        GateDriverStatus status = new HttpGateDriver(properties, new ObjectMapper()).status(MovementDirection.EXIT);

        assertThat(status.state()).isEqualTo(GateDriverState.NOT_CONFIGURED);
    }
}
