package com.estacionamento.api.observability;

public record ObservabilitySummaryResponse(
        String application,
        String status,
        String metricsEndpoint,
        String healthEndpoint,
        int realtimeConnectedClients
) {
}
