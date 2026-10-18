package com.estacionamento.api.application.movement;

import com.estacionamento.api.application.audit.AuditService;
import com.estacionamento.api.application.gate.GateOperationService;
import com.estacionamento.api.application.realtime.RealtimeEventPublisher;
import com.estacionamento.api.domain.camera.Camera;
import com.estacionamento.api.domain.camera.CameraDirection;
import com.estacionamento.api.domain.camera.CameraRepository;
import com.estacionamento.api.domain.hikvision.HikvisionEvent;
import com.estacionamento.api.domain.hikvision.HikvisionEventRepository;
import com.estacionamento.api.domain.movement.AccessDecision;
import com.estacionamento.api.domain.movement.Movement;
import com.estacionamento.api.domain.movement.MovementDirection;
import com.estacionamento.api.domain.movement.MovementRepository;
import com.estacionamento.api.domain.movement.MovementSource;
import com.estacionamento.api.domain.movement.MovementStatus;
import com.estacionamento.api.domain.parking.ParkingSpace;
import com.estacionamento.api.domain.parking.ParkingSpaceRepository;
import com.estacionamento.api.domain.parking.ParkingSpaceStatus;
import com.estacionamento.api.domain.parking.ParkingSpaceType;
import com.estacionamento.api.domain.user.User;
import com.estacionamento.api.domain.user.UserRepository;
import com.estacionamento.api.domain.vehicle.Vehicle;
import com.estacionamento.api.domain.vehicle.VehicleRepository;
import com.estacionamento.api.infrastructure.hikvision.PlateNormalizer;
import com.estacionamento.api.observability.GarageMetrics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;

@Service
public class MovementService {

    private final VehicleRepository vehicleRepository;
    private final ParkingSpaceRepository parkingSpaceRepository;
    private final CameraRepository cameraRepository;
    private final HikvisionEventRepository hikvisionEventRepository;
    private final MovementRepository movementRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final GateOperationService gateOperationService;
    private final PlateNormalizer plateNormalizer;
    private final Clock clock;
    private final int minimumConfidence;
    private final RealtimeEventPublisher realtimeEventPublisher;
    private final GarageMetrics metrics;

    public MovementService(VehicleRepository vehicleRepository,
                           ParkingSpaceRepository parkingSpaceRepository,
                           CameraRepository cameraRepository,
                           HikvisionEventRepository hikvisionEventRepository,
                           MovementRepository movementRepository,
                           UserRepository userRepository,
                           AuditService auditService,
                           GateOperationService gateOperationService,
                           PlateNormalizer plateNormalizer,
                           Clock clock,
                           int minimumConfidence,
                           RealtimeEventPublisher realtimeEventPublisher) {
        this(vehicleRepository, parkingSpaceRepository, cameraRepository, hikvisionEventRepository, movementRepository, userRepository, auditService, gateOperationService, plateNormalizer, clock, minimumConfidence, realtimeEventPublisher, new com.estacionamento.api.observability.GarageMetrics());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public MovementService(VehicleRepository vehicleRepository,
                           ParkingSpaceRepository parkingSpaceRepository,
                           CameraRepository cameraRepository,
                           HikvisionEventRepository hikvisionEventRepository,
                           MovementRepository movementRepository,
                           UserRepository userRepository,
                           AuditService auditService,
                           GateOperationService gateOperationService,
                           PlateNormalizer plateNormalizer,
                           Clock clock,
                           @Value("${app.parking.access.min-confidence:80}") int minimumConfidence,
                           RealtimeEventPublisher realtimeEventPublisher,
                           GarageMetrics metrics) {
        this.vehicleRepository = vehicleRepository;
        this.parkingSpaceRepository = parkingSpaceRepository;
        this.cameraRepository = cameraRepository;
        this.hikvisionEventRepository = hikvisionEventRepository;
        this.movementRepository = movementRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.gateOperationService = gateOperationService;
        this.plateNormalizer = plateNormalizer;
        this.clock = clock;
        this.minimumConfidence = minimumConfidence;
        this.realtimeEventPublisher = realtimeEventPublisher;
        this.metrics = metrics;
    }

    @Transactional
    public MovementResult processAutomatic(Long hikvisionEventId, Long cameraId, String plate,
                                           Integer confidence, OffsetDateTime eventTime) {
        Camera camera = cameraRepository.findById(cameraId)
                .orElseThrow(() -> new IllegalStateException("Camera not found: " + cameraId));
        MovementDirection direction = toMovementDirection(camera.getDirection());
        return process(
                hikvisionEventId,
                camera,
                plate,
                confidence,
                eventTime,
                direction,
                MovementSource.HIKVISION,
                null,
                null,
                false
        );
    }

    @Transactional
    public MovementResult createManual(String plate, MovementDirection direction, String justification, Long operatorUserId) {
        requireJustification(justification);
        User operator = userRepository.findById(operatorUserId)
                .orElseThrow(() -> new IllegalArgumentException("Operator user not found: " + operatorUserId));
        if (!operator.isActive() || !operator.getRole().isActive()) {
            throw new IllegalArgumentException("Operator user is inactive");
        }
        return process(null, null, plate, null, OffsetDateTime.now(clock), direction,
                MovementSource.MANUAL, operator, justification, true);
    }

    @Transactional
    public MovementResult override(Long movementId, String justification, Long operatorUserId) {
        requireJustification(justification);
        User operator = userRepository.findById(operatorUserId)
                .orElseThrow(() -> new IllegalArgumentException("Operator user not found: " + operatorUserId));
        if (!operator.isActive() || !operator.getRole().isActive()) {
            throw new IllegalArgumentException("Operator user is inactive");
        }
        Movement movement = movementRepository.findByIdForUpdate(movementId)
                .orElseThrow(() -> new IllegalArgumentException("Movement not found: " + movementId));

        if (movement.getStatus() != MovementStatus.DENIED) {
            throw new IllegalArgumentException("Only denied movements can be overridden");
        }

        Vehicle vehicle = vehicleRepository.findByPlateForUpdate(movement.getPlate())
                .orElseThrow(() -> new IllegalStateException("Register the vehicle before authorizing an override"));
        ParkingSpace space = null;
        if (vehicle.getParkingSpace() != null) {
            space = parkingSpaceRepository.findByIdForUpdate(vehicle.getParkingSpace().getId()).orElse(null);
        }

        if (movementRepository.hasUnresolvedPassageForPlate(vehicle.getPlate())) {
            throw new IllegalStateException("A passage is already awaiting reconciliation for this vehicle");
        }
        if (movement.getDirection() == MovementDirection.ENTRY && (vehicle.isInsideGarage() || space == null
                || space.getStatus() != ParkingSpaceStatus.AVAILABLE
                || movementRepository.hasUnresolvedPassageForSpace(space.getId()))) {
            throw new IllegalStateException("Resolve the entry occupancy or reservation conflict before authorizing an override");
        }
        if (movement.getDirection() == MovementDirection.EXIT && !vehicle.isInsideGarage()) {
            throw new IllegalStateException("No confirmed entry exists for this vehicle");
        }
        movement.override(operator, justification, vehicle, space);
        movement.reservePassage();
        Movement saved = movementRepository.save(movement);
        realtimeEventPublisher.publish("movement.created", MovementResult.from(saved));
        gateOperationService.requestOpen(saved);
        auditService.record(operator, "MOVEMENT_OVERRIDE", "MOVEMENT", movementId.toString(), justification,
                "plate=" + movement.getPlate() + ", direction=" + movement.getDirection() + ", passageConfirmationRequired=true");
        return MovementResult.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<Movement> list(Pageable pageable) {
        return movementRepository.findAllByOrderByEventTimeDesc(pageable);
    }

    @Transactional(readOnly = true)
    public Movement get(Long id) {
        return movementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Movement not found: " + id));
    }

    private MovementResult process(Long hikvisionEventId, Camera camera, String rawPlate,
                                   Integer confidence, OffsetDateTime eventTime, MovementDirection direction,
                                   MovementSource source, User operator, String justification,
                                   boolean auditManual) {
        String plate = plateNormalizer.normalize(rawPlate);
        if (plate == null) {
            throw new IllegalArgumentException("Vehicle plate is required");
        }

        if (source == MovementSource.HIKVISION && (confidence == null || confidence < minimumConfidence)) {
            HikvisionEvent hikvisionEvent = hikvisionEventId == null ? null : hikvisionEventRepository.findById(hikvisionEventId).orElse(null);
            Movement movement = new Movement(null, camera, null, hikvisionEvent, operator, plate, direction,
                    source, MovementStatus.DENIED, AccessDecision.LOW_CONFIDENCE, confidence, eventTime, justification);
            MovementResult result = MovementResult.from(movementRepository.save(movement));
            metrics.movementDenied(direction);
            metrics.movementDecision(result.decision());
            realtimeEventPublisher.publish("movement.created", result);
            return result;
        }

        Vehicle vehicle = vehicleRepository.findByPlateForUpdate(plate).orElse(null);
        AccessDecision decision;
        ParkingSpace parkingSpace = null;

        if (vehicle == null) {
            decision = AccessDecision.UNKNOWN_VEHICLE;
        } else if (movementRepository.hasUnresolvedPassageForPlate(plate)) {
            decision = AccessDecision.OPERATION_PENDING;
        } else if (direction == MovementDirection.EXIT) {
            decision = evaluateExit(vehicle);
            parkingSpace = vehicle.getParkingSpace();
            if (decision == AccessDecision.AUTHORIZED) {
                if (parkingSpace != null) {
                    parkingSpace = parkingSpaceRepository.findByIdForUpdate(parkingSpace.getId()).orElse(null);
                }
                // Occupancy changes only after an explicit passage confirmation.
            } else if (!vehicle.isActive()) {
                decision = AccessDecision.VEHICLE_INACTIVE;
            }
        } else if (!vehicle.isActive()) {
            decision = AccessDecision.VEHICLE_INACTIVE;
        } else if (direction == MovementDirection.ENTRY) {
            decision = evaluateEntry(vehicle);
            if (decision == AccessDecision.AUTHORIZED) {
                parkingSpace = parkingSpaceRepository.findByIdForUpdate(vehicle.getParkingSpace().getId())
                        .orElseThrow(() -> new IllegalStateException("Assigned parking space not found"));
                if (parkingSpace.getStatus() != ParkingSpaceStatus.AVAILABLE) {
                    decision = AccessDecision.PARKING_SPACE_OCCUPIED;
                } else if (movementRepository.hasUnresolvedPassageForSpace(parkingSpace.getId())) {
                    decision = AccessDecision.OPERATION_PENDING;
                }
            }
        } else {
            decision = AccessDecision.INVALID_DIRECTION;
        }

        MovementStatus status = decision == AccessDecision.AUTHORIZED
                ? MovementStatus.AUTHORIZED : MovementStatus.DENIED;
        HikvisionEvent hikvisionEvent = hikvisionEventId == null ? null : hikvisionEventRepository.findById(hikvisionEventId).orElse(null);
        Movement movement = new Movement(vehicle, camera, parkingSpace, hikvisionEvent, operator, plate,
                direction, source, status, source == MovementSource.MANUAL && decision == AccessDecision.AUTHORIZED
                ? AccessDecision.MANUAL_AUTHORIZED : decision, confidence, eventTime, justification);
        if (status == MovementStatus.AUTHORIZED) movement.reservePassage();
        Movement saved = movementRepository.save(movement);
        metrics.movementDecision(saved.getAccessDecision());
        if (saved.getStatus() == MovementStatus.AUTHORIZED || saved.getStatus() == MovementStatus.OVERRIDDEN) {
            metrics.movementAuthorized(direction);
        } else {
            metrics.movementDenied(direction);
        }

        if (saved.getStatus() == MovementStatus.AUTHORIZED || saved.getStatus() == MovementStatus.OVERRIDDEN) {
            gateOperationService.requestOpen(saved);
        }

        if (auditManual) {
            auditService.record(operator, "MOVEMENT_MANUAL", "MOVEMENT", saved.getId().toString(), justification,
                    "plate=" + plate + ", direction=" + direction + ", decision=" + saved.getAccessDecision());
        }
        realtimeEventPublisher.publish("movement.created", MovementResult.from(saved));
        return MovementResult.from(saved);
    }

    private AccessDecision evaluateEntry(Vehicle vehicle) {
        if (vehicle.isInsideGarage()) {
            return AccessDecision.DUPLICATE_ENTRY;
        }
        if (vehicle.getResidents().stream().noneMatch(r -> r.isActive() && r.getApartment() != null && r.getApartment().isActive())) {
            return AccessDecision.RESIDENT_INACTIVE;
        }
        ParkingSpace space = vehicle.getParkingSpace();
        if (space == null) {
            return AccessDecision.NO_PARKING_ASSIGNED;
        }
        if (!space.isActive()) {
            return AccessDecision.PARKING_SPACE_INACTIVE;
        }
        if (space.getType() == ParkingSpaceType.VISITOR) {
            return AccessDecision.NO_PARKING_ASSIGNED;
        }
        if (space.getStatus() == ParkingSpaceStatus.BLOCKED) {
            return AccessDecision.PARKING_SPACE_BLOCKED;
        }
        if (space.getStatus() == ParkingSpaceStatus.OCCUPIED) {
            return AccessDecision.PARKING_SPACE_OCCUPIED;
        }
        return AccessDecision.AUTHORIZED;
    }

    private AccessDecision evaluateExit(Vehicle vehicle) {
        return vehicle.isInsideGarage() ? AccessDecision.AUTHORIZED : AccessDecision.EXIT_WITHOUT_ENTRY;
    }

    @Transactional
    public MovementResult confirmPassage(Long id, boolean passed, String justification, Long operatorId) {
        return confirmPassage(id, passed, justification, operatorId, false);
    }

    @Transactional
    public MovementResult confirmPassage(Long id, boolean passed, String justification, Long operatorId, boolean controllerSettled) {
        requireJustification(justification);
        User operator = userRepository.findById(operatorId).filter(u -> u.isActive() && u.getRole().isActive())
                .orElseThrow(() -> new IllegalArgumentException("Operator is inactive"));
        Movement movement = movementRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Movement not found"));
        if ((movement.getPassageState() == com.estacionamento.api.domain.movement.PassageState.COMPLETED && passed) || (movement.getPassageState() == com.estacionamento.api.domain.movement.PassageState.FAILED && !passed)) return MovementResult.from(movement);
        // Never cancel a command while it may still be dispatched.
        gateOperationService.assertReconciliationAllowed(id, controllerSettled);
        Vehicle vehicle = vehicleRepository.findByPlateForUpdate(movement.getPlate()).orElse(null);
        ParkingSpace space = movement.getParkingSpace() == null ? null
                : parkingSpaceRepository.findByIdForUpdate(movement.getParkingSpace().getId()).orElse(null);
        if (passed) {
            if (vehicle == null) throw new IllegalStateException("Unknown vehicle: reconcile its registration first");
            if (movement.getDirection() == MovementDirection.ENTRY && (vehicle.isInsideGarage() || space == null
                    || space.getStatus() != ParkingSpaceStatus.AVAILABLE)) throw new IllegalStateException("Occupancy conflicts with the observed entry");
            if (movement.getDirection() == MovementDirection.EXIT && !vehicle.isInsideGarage()) throw new IllegalStateException("No confirmed entry exists");
            applyStateForAuthorizedMovement(vehicle, space, movement.getDirection());
        }
        movement.reconcile(passed, OffsetDateTime.now(clock));
        auditService.record(operator, "PASSAGE_RECONCILIATION", "MOVEMENT", id.toString(), justification, "passed=" + passed + ", controllerSettled=" + controllerSettled + ", source=OPERATOR_OBSERVED");
        realtimeEventPublisher.publish("movement.updated", MovementResult.from(movement));
        return MovementResult.from(movement);
    }

    private void applyStateForAuthorizedMovement(Vehicle vehicle, ParkingSpace parkingSpace, MovementDirection direction) {
        if (direction == MovementDirection.ENTRY) {
            if (parkingSpace == null) {
                throw new IllegalStateException("Authorized entry requires parking space");
            }
            parkingSpace.markOccupied();
            vehicle.markInsideGarage();
        } else {
            if (parkingSpace != null) {
                parkingSpace.markAvailable();
            }
            vehicle.markOutsideGarage();
        }
    }

    private MovementDirection toMovementDirection(CameraDirection direction) {
        return direction == CameraDirection.ENTRY ? MovementDirection.ENTRY : MovementDirection.EXIT;
    }

    private void requireJustification(String justification) {
        if (justification == null || justification.trim().length() < 5) {
            throw new IllegalArgumentException("Justification must contain at least 5 characters");
        }
    }
}
