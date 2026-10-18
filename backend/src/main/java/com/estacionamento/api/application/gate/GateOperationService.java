package com.estacionamento.api.application.gate;

import com.estacionamento.api.application.audit.AuditService;
import com.estacionamento.api.application.realtime.RealtimeEventPublisher;
import com.estacionamento.api.config.properties.GateProperties;
import com.estacionamento.api.domain.gate.GateOperation;
import com.estacionamento.api.domain.gate.GateOperationRepository;
import com.estacionamento.api.domain.gate.GateOperationStatus;
import com.estacionamento.api.domain.movement.Movement;
import com.estacionamento.api.domain.movement.MovementDirection;
import com.estacionamento.api.domain.user.User;
import com.estacionamento.api.infrastructure.gate.GateCommandResult;
import com.estacionamento.api.infrastructure.gate.GateDriver;
import com.estacionamento.api.infrastructure.gate.GateOpenCommand;
import com.estacionamento.api.infrastructure.gate.GateDriverStatus;
import com.estacionamento.api.observability.GarageMetrics;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Map;

@Service
public class GateOperationService {

    private final GateOperationRepository repository;
    private final GateProperties properties;
    private final Map<String, GateDriver> drivers;
    private final Clock clock;
    private final AuditService auditService;
    private final RealtimeEventPublisher realtimeEventPublisher;
    private final GarageMetrics metrics;
    private final com.estacionamento.api.domain.movement.MovementRepository movements;

    public GateOperationService(GateOperationRepository repository,
                                GateProperties properties,
                                Map<String, GateDriver> drivers,
                                Clock clock,
                                AuditService auditService,
                                RealtimeEventPublisher realtimeEventPublisher) {
        this(repository, properties, drivers, clock, auditService, realtimeEventPublisher, new com.estacionamento.api.observability.GarageMetrics(), null);
    }

    public GateOperationService(GateOperationRepository repository, GateProperties properties, Map<String, GateDriver> drivers, Clock clock, AuditService auditService, RealtimeEventPublisher events, GarageMetrics metrics) {
        this(repository, properties, drivers, clock, auditService, events, metrics, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public GateOperationService(GateOperationRepository repository,
                                GateProperties properties,
                                Map<String, GateDriver> drivers,
                                Clock clock,
                                AuditService auditService,
                                RealtimeEventPublisher realtimeEventPublisher,
                                GarageMetrics metrics,
                                com.estacionamento.api.domain.movement.MovementRepository movements) {
        this.repository = repository;
        this.movements = movements;
        this.properties = properties;
        this.drivers = drivers;
        this.clock = clock;
        this.auditService = auditService;
        this.realtimeEventPublisher = realtimeEventPublisher;
        this.metrics = metrics;
    }

    @Transactional
    public GateOperation requestOpen(Movement movement) {
        if (movement.getStatus() != com.estacionamento.api.domain.movement.MovementStatus.AUTHORIZED && movement.getStatus() != com.estacionamento.api.domain.movement.MovementStatus.OVERRIDDEN) throw new IllegalArgumentException("Denied movements cannot request gate commands");
        return repository.findByMovementId(movement.getId()).orElseGet(() -> {
            OffsetDateTime now = OffsetDateTime.now(clock);
            metrics.gateRequested();
            GateOperation operation = properties.enabled()
                    ? GateOperation.pending(movement, properties.driver(), now)
                    : GateOperation.disabled(movement, properties.driver(), now);
            movement.passageState(properties.enabled() ? com.estacionamento.api.domain.movement.PassageState.GATE_REQUESTED : com.estacionamento.api.domain.movement.PassageState.RECONCILIATION_REQUIRED);
            GateOperation saved = repository.save(operation);
            realtimeEventPublisher.publish("gate.updated", GateOperationResponse.from(saved));
            return saved;
        });
    }

    @Transactional(readOnly = true)
    public Page<GateOperation> list(Pageable pageable) {
        return repository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional
    public void assertReconciliationAllowed(Long movementId, boolean controllerSettled) {
        repository.lockByMovementId(movementId).ifPresent(operation -> {
            if (operation.getStatus() == GateOperationStatus.PENDING || operation.getStatus() == GateOperationStatus.PROCESSING)
                throw new IllegalStateException("Wait until the pending command is resolved before confirming passage");
            if (operation.getStatus() == GateOperationStatus.UNKNOWN && !controllerSettled) throw new IllegalStateException("Verify the controller has no outstanding command before reconciling an unknown outcome");
        });
    }

    @Transactional
    public GateOperation retryNow(Long operationId, User operator, String justification) {
        Long movementId = repository.findMovementId(operationId).orElseThrow(() -> new IllegalArgumentException("Gate operation not found"));
        Movement movement = movements.findByIdForUpdate(movementId).orElseThrow();
        if (!movement.isReservationActive() || movement.getPassageState() == com.estacionamento.api.domain.movement.PassageState.COMPLETED
                || movement.getPassageState() == com.estacionamento.api.domain.movement.PassageState.FAILED) throw new IllegalStateException("Passage has already been reconciled");
        GateOperation operation = repository.findLocked(operationId).orElseThrow();
        if (operation.getStatus() != GateOperationStatus.EXHAUSTED) {
            throw new IllegalArgumentException("Only exhausted gate operations can be retried manually");
        }
        if (operation.getMovement().getId() == null) {
            throw new IllegalStateException("Gate operation is not linked to a persisted movement");
        }
        movement.passageState(com.estacionamento.api.domain.movement.PassageState.GATE_REQUESTED);
        operation.markFailure("Manual retry requested", OffsetDateTime.now(clock), false);
        GateOperation saved = repository.save(operation);
        realtimeEventPublisher.publish("gate.updated", GateOperationResponse.from(saved));
        auditService.record(operator, "GATE_OPERATION_RETRY", "GATE_OPERATION", operationId.toString(), justification,
                "movementId=" + operation.getMovement().getId() + ", plate=" + operation.getMovement().getPlate());
        return operation;
    }

    @Transactional
    public void test(MovementDirection direction, User operator, String justification) {
        if (!properties.enabled()) {
            throw new IllegalStateException("Gate control is disabled");
        }
        if (!"SIMULATED".equalsIgnoreCase(properties.driver())) throw new IllegalStateException("Physical tests require an approved controller commissioning procedure");
        GateDriver driver = resolveDriver(properties.driver());
        GateCommandResult result = driver.open(new GateOpenCommand(
                "TEST-" + java.util.UUID.randomUUID(), direction, "TEST", properties.openPulseMillis()));
        auditService.record(operator, "GATE_TEST", "GATE", direction.name(), justification,
                "driver=" + properties.driver() + ", success=" + result.success() + ", message=" + result.message());
        if (!result.success()) {
            throw new IllegalStateException(result.message());
        }
    }

    @Transactional(readOnly = true)
    public GateStatusResponse status() {
        if (!properties.enabled()) {
            return new GateStatusResponse(false, properties.driver(), false, false,
                    com.estacionamento.api.infrastructure.gate.GateDriverState.DISABLED,
                    "Gate control is disabled",
                    com.estacionamento.api.infrastructure.gate.GateDriverState.DISABLED,
                    "Gate control is disabled",
                    "SAFE_DISABLED");
        }

        GateDriver driver = resolveDriver(properties.driver());
        GateDriverStatus entry = driver.status(MovementDirection.ENTRY);
        GateDriverStatus exit = driver.status(MovementDirection.EXIT);
        return new GateStatusResponse(
                true,
                properties.driver(),
                entry.state() == com.estacionamento.api.infrastructure.gate.GateDriverState.READY
                        || entry.state() == com.estacionamento.api.infrastructure.gate.GateDriverState.CONFIGURED,
                exit.state() == com.estacionamento.api.infrastructure.gate.GateDriverState.READY
                        || exit.state() == com.estacionamento.api.infrastructure.gate.GateDriverState.CONFIGURED,
                entry.state(), entry.message(), exit.state(), exit.message(),
                properties.driver().equalsIgnoreCase("SIMULATED") ? "SIMULATED" : "HARDWARE_ADAPTER");
    }

    private GateDriver resolveDriver(String name) {
        GateDriver driver = drivers.get(name);
        if (driver == null) {
            throw new IllegalStateException("Unsupported gate driver: " + name);
        }
        return driver;
    }
}
