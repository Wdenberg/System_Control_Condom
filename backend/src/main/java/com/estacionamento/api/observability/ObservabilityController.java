package com.estacionamento.api.observability;

import com.estacionamento.api.application.realtime.RealtimeBroadcaster;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system/observability")
public class ObservabilityController {

    private final RealtimeBroadcaster realtimeBroadcaster;

    public ObservabilityController(RealtimeBroadcaster realtimeBroadcaster) {
        this.realtimeBroadcaster = realtimeBroadcaster;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('DASHBOARD_READ')")
    public ObservabilitySummaryResponse summary() {
        return new ObservabilitySummaryResponse(
                "garage-control-backend",
                "NOT_CHECKED",
                "/actuator/prometheus",
                "/actuator/health",
                realtimeBroadcaster.connectedClients()
        );
    }
}
