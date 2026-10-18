package com.estacionamento.api.observability;

import com.estacionamento.api.domain.movement.AccessDecision;
import com.estacionamento.api.domain.movement.MovementDirection;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.util.EnumMap;
import java.util.Map;

@Component
public class GarageMetrics {

    private final MeterRegistry registry;
    private final Counter hikvisionReceived;
    private final Counter hikvisionDuplicate;
    private final Counter hikvisionCooldown;
    private final Counter hikvisionFailed;
    private final Timer hikvisionProcessing;
    private final Map<MovementDirection, Counter> movementAuthorized = new EnumMap<>(MovementDirection.class);
    private final Map<MovementDirection, Counter> movementDenied = new EnumMap<>(MovementDirection.class);
    private final Map<AccessDecision, Counter> movementDecisions = new EnumMap<>(AccessDecision.class);
    private final Counter gateRequested;
    private final Counter gateSucceeded;
    private final Counter gateFailed;
    private final Counter gateExhausted;
    private final Counter gateUnknown;
    private final Counter realtimeEvents;
    private final Counter realtimeConnections;
    private final Counter realtimeDisconnects;

    public GarageMetrics() {
        this(new SimpleMeterRegistry());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public GarageMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.hikvisionReceived = counter("garage_hikvision_events_received_total", "Hikvision ANPR events accepted for processing");
        this.hikvisionDuplicate = counter("garage_hikvision_events_duplicate_total", "Hikvision ANPR events identified as duplicates");
        this.hikvisionCooldown = counter("garage_hikvision_events_cooldown_total", "Hikvision ANPR events rejected by cooldown");
        this.hikvisionFailed = counter("garage_hikvision_events_failed_total", "Hikvision ANPR events that failed processing");
        this.hikvisionProcessing = Timer.builder("garage_hikvision_processing_seconds")
                .description("Hikvision ANPR event processing duration")
                .publishPercentileHistogram()
                .register(registry);
        for (MovementDirection direction : MovementDirection.values()) {
            movementAuthorized.put(direction, Counter.builder("garage_movements_authorized_total")
                    .description("Authorized garage movements")
                    .tag("direction", direction.name().toLowerCase())
                    .register(registry));
            movementDenied.put(direction, Counter.builder("garage_movements_denied_total")
                    .description("Denied garage movements")
                    .tag("direction", direction.name().toLowerCase())
                    .register(registry));
        }
        for (AccessDecision decision : AccessDecision.values()) {
            movementDecisions.put(decision, Counter.builder("garage_movement_decisions_total")
                    .description("Garage movement decisions")
                    .tag("decision", decision.name())
                    .register(registry));
        }
        gateRequested = counter("garage_gate_operations_requested_total", "Gate open operations requested");
        gateSucceeded = counter("garage_gate_operations_succeeded_total", "Gate open operations successfully sent");
        gateFailed = counter("garage_gate_operations_failed_total", "Gate open operations failed");
        gateExhausted = counter("garage_gate_operations_exhausted_total", "Gate operations exhausted after retries");
        gateUnknown = counter("garage_gate_operations_unknown_total", "Gate attempts whose physical outcome requires reconciliation");
        realtimeEvents = counter("garage_realtime_events_broadcast_total", "Realtime events broadcast to connected clients");
        realtimeConnections = counter("garage_realtime_connections_total", "Realtime SSE connections created");
        realtimeDisconnects = counter("garage_realtime_disconnects_total", "Realtime SSE connections closed");
    }

    private Counter counter(String name, String description) {
        return Counter.builder(name).description(description).register(registry);
    }

    public Timer.Sample startHikvisionProcessing() {
        return Timer.start(registry);
    }

    public void stopHikvisionProcessing(Timer.Sample sample) {
        sample.stop(hikvisionProcessing);
    }

    public void hikvisionReceived() {
        hikvisionReceived.increment();
    }

    public void hikvisionDuplicate() {
        hikvisionDuplicate.increment();
    }

    public void hikvisionCooldown() {
        hikvisionCooldown.increment();
    }

    public void hikvisionFailed() {
        hikvisionFailed.increment();
    }

    public void movementAuthorized(MovementDirection direction) {
        movementAuthorized.get(direction).increment();
    }

    public void movementDenied(MovementDirection direction) {
        movementDenied.get(direction).increment();
    }

    public void movementDecision(AccessDecision decision) {
        movementDecisions.get(decision).increment();
    }

    public void gateRequested() {
        gateRequested.increment();
    }

    public void gateSucceeded() {
        gateSucceeded.increment();
    }

    public void gateFailed() {
        gateFailed.increment();
    }

    public void gateExhausted() {
        gateExhausted.increment();
    }

    @org.springframework.transaction.event.TransactionalEventListener(phase = org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT)
    public void gateAttemptFinished(com.estacionamento.api.application.gate.GateDispatchTransactions.AttemptFinished event) {
        switch (event.outcome()) {
            case SENT -> gateSucceeded();
            case EXHAUSTED -> { gateFailed(); gateExhausted(); }
            case UNKNOWN -> { gateFailed(); gateUnknown.increment(); }
            case PENDING -> gateFailed();
            default -> { }
        }
    }

    public void realtimeEventBroadcast() {
        realtimeEvents.increment();
    }

    public void realtimeConnectionOpened() {
        realtimeConnections.increment();
    }

    public void realtimeConnectionClosed() {
        realtimeDisconnects.increment();
    }
}
