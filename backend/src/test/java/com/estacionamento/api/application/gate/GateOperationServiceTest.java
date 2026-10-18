package com.estacionamento.api.application.gate;

import com.estacionamento.api.application.audit.AuditService;
import com.estacionamento.api.application.realtime.RealtimeEventPublisher;
import com.estacionamento.api.config.properties.GateProperties;
import com.estacionamento.api.domain.gate.GateOperation;
import com.estacionamento.api.domain.gate.GateOperationRepository;
import com.estacionamento.api.domain.gate.GateOperationStatus;
import com.estacionamento.api.domain.movement.Movement;
import com.estacionamento.api.domain.movement.MovementDirection;
import com.estacionamento.api.domain.movement.MovementSource;
import com.estacionamento.api.domain.movement.MovementStatus;
import com.estacionamento.api.domain.movement.AccessDecision;
import com.estacionamento.api.infrastructure.gate.GateDriver;
import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GateOperationServiceTest {

    @Test
    void shouldCreateDisabledOperationWhenGateIsDisabled() {
        GateOperationRepository repository = mock(GateOperationRepository.class);
        AuditService auditService = mock(AuditService.class);
        RealtimeEventPublisher realtimeEventPublisher = mock(RealtimeEventPublisher.class);
        GateProperties properties = properties(false, "SIMULATED");
        Movement movement = movement();
        when(repository.findByMovementId(null)).thenReturn(Optional.empty());
        when(repository.save(any(GateOperation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GateOperationService service = new GateOperationService(
                repository, properties, Map.of("SIMULATED", command -> null), fixedClock(), auditService, realtimeEventPublisher);

        GateOperation operation = service.requestOpen(movement);

        assertThat(operation.getStatus()).isEqualTo(GateOperationStatus.DISABLED);
        verify(repository).save(any(GateOperation.class));
    }

    @Test
    void shouldReportSafeSimulatedStatusWhenEnabled() {
        GateOperationRepository repository = mock(GateOperationRepository.class);
        AuditService auditService = mock(AuditService.class);
        RealtimeEventPublisher realtimeEventPublisher = mock(RealtimeEventPublisher.class);
        GateProperties properties = new GateProperties(true, "SIMULATED", "", "", "", "", "",
                Duration.ofSeconds(2), 3, Duration.ofSeconds(1), false, 1000);

        GateOperationService service = new GateOperationService(
                repository, properties, Map.of("SIMULATED", new com.estacionamento.api.infrastructure.gate.SimulatedGateDriver()),
                fixedClock(), auditService, realtimeEventPublisher);

        GateStatusResponse status = service.status();

        assertThat(status.enabled()).isTrue();
        assertThat(status.safetyMode()).isEqualTo("SIMULATED");
        assertThat(status.entryState()).isEqualTo(com.estacionamento.api.infrastructure.gate.GateDriverState.READY);
    }

    @Test
    void shouldDispatchSuccessfulOperation() {
        GateDispatchTransactions transactions = mock(GateDispatchTransactions.class);
        var claim = new GateDispatchTransactions.Claim(1L, "claim", "SIMULATED", new com.estacionamento.api.infrastructure.gate.GateOpenCommand("operation", MovementDirection.ENTRY, "ABC1D23", 1000));
        when(transactions.claim()).thenReturn(Optional.of(claim), Optional.empty());
        GateDriver driver = command -> com.estacionamento.api.infrastructure.gate.GateCommandResult.success("ok");
        new GateDispatchWorker(transactions, Map.of("SIMULATED", driver)).dispatchPending();
        verify(transactions).finish(eq(claim), argThat(result -> result.success() && !result.uncertain()));
    }
    @Test
    void driverExceptionMustBecomeUnknownWithoutBlindRetry() {
        GateDispatchTransactions transactions = mock(GateDispatchTransactions.class);
        var claim = new GateDispatchTransactions.Claim(1L, "claim", "HTTP", new com.estacionamento.api.infrastructure.gate.GateOpenCommand("operation", MovementDirection.ENTRY, "ABC1D23", 1000));
        when(transactions.claim()).thenReturn(Optional.of(claim), Optional.empty());
        GateDriver driver = command -> { throw new IllegalStateException("Simulated uncertain delivery"); };
        new GateDispatchWorker(transactions, Map.of("HTTP", driver)).dispatchPending();
        verify(transactions).finish(eq(claim), argThat(result -> result.uncertain() && !result.success()));
    }

    private GateProperties properties(boolean enabled, String driver) {
        return new GateProperties(enabled, driver, "", "", "", "", "",
                Duration.ofSeconds(2), 3, Duration.ofSeconds(1), false, 1000);
    }

    private Clock fixedClock() {
        return Clock.fixed(Instant.parse("2026-09-13T10:00:00Z"), ZoneOffset.UTC);
    }

    private Movement movement() {
        return new Movement(null, null, null, null, null, "ABC1D23", MovementDirection.ENTRY,
                MovementSource.MANUAL, MovementStatus.AUTHORIZED, AccessDecision.AUTHORIZED,
                null, Instant.parse("2026-09-13T10:00:00Z").atOffset(ZoneOffset.UTC), "manual test");
    }
}
