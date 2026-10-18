package com.estacionamento.api.application.dashboard;

import com.estacionamento.api.application.movement.MovementResult;
import com.estacionamento.api.config.properties.DashboardProperties;
import com.estacionamento.api.domain.apartment.ApartmentRepository;
import com.estacionamento.api.domain.camera.Camera;
import com.estacionamento.api.domain.camera.CameraRepository;
import com.estacionamento.api.domain.gate.GateOperation;
import com.estacionamento.api.domain.gate.GateOperationRepository;
import com.estacionamento.api.domain.gate.GateOperationStatus;
import com.estacionamento.api.domain.hikvision.HikvisionEvent;
import com.estacionamento.api.domain.hikvision.HikvisionEventRepository;
import com.estacionamento.api.domain.movement.AccessDecision;
import com.estacionamento.api.domain.movement.Movement;
import com.estacionamento.api.domain.movement.MovementDirection;
import com.estacionamento.api.domain.movement.MovementRepository;
import com.estacionamento.api.domain.movement.MovementStatus;
import com.estacionamento.api.domain.parking.ParkingSpace;
import com.estacionamento.api.domain.parking.ParkingSpaceRepository;
import com.estacionamento.api.domain.parking.ParkingSpaceStatus;
import com.estacionamento.api.domain.resident.ResidentRepository;
import com.estacionamento.api.domain.vehicle.Vehicle;
import com.estacionamento.api.domain.vehicle.VehicleRepository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class DashboardService {

    private static final String SUMMARY_KEY = "dashboard:summary:v1";
    private static final String ACTIVITY_KEY = "dashboard:activity:v1";
    private static final String OCCUPANCY_KEY = "dashboard:occupancy:v1";
    private static final String CAMERAS_KEY = "dashboard:cameras:v1";
    private static final String ALERTS_KEY = "dashboard:alerts:v1";

    private final ApartmentRepository apartmentRepository;
    private final ResidentRepository residentRepository;
    private final VehicleRepository vehicleRepository;
    private final ParkingSpaceRepository parkingSpaceRepository;
    private final MovementRepository movementRepository;
    private final GateOperationRepository gateOperationRepository;
    private final CameraRepository cameraRepository;
    private final HikvisionEventRepository hikvisionEventRepository;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final DashboardProperties properties;
    private final Clock clock;

    public DashboardService(ApartmentRepository apartmentRepository,
                            ResidentRepository residentRepository,
                            VehicleRepository vehicleRepository,
                            ParkingSpaceRepository parkingSpaceRepository,
                            MovementRepository movementRepository,
                            GateOperationRepository gateOperationRepository,
                            CameraRepository cameraRepository,
                            HikvisionEventRepository hikvisionEventRepository,
                            ObjectMapper objectMapper,
                            StringRedisTemplate redisTemplate,
                            DashboardProperties properties,
                            Clock clock) {
        this.apartmentRepository = apartmentRepository;
        this.residentRepository = residentRepository;
        this.vehicleRepository = vehicleRepository;
        this.parkingSpaceRepository = parkingSpaceRepository;
        this.movementRepository = movementRepository;
        this.gateOperationRepository = gateOperationRepository;
        this.cameraRepository = cameraRepository;
        this.hikvisionEventRepository = hikvisionEventRepository;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary() {
        return cached(SUMMARY_KEY, DashboardSummaryResponse.class, this::loadSummary);
    }

    @Transactional(readOnly = true)
    public List<DashboardActivityResponse> recentActivity(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        String key = ACTIVITY_KEY + ":" + safeLimit;
        return cached(key, new TypeReference<List<DashboardActivityResponse>>() {}, () -> movementRepository
                .findAllByOrderByEventTimeDesc(org.springframework.data.domain.PageRequest.of(0, safeLimit))
                .getContent()
                .stream()
                .map(MovementResult::from)
                .map(DashboardActivityResponse::from)
                .toList());
    }

    @Transactional(readOnly = true)
    public List<DashboardOccupancyResponse> occupancy() {
        return cached(OCCUPANCY_KEY, new TypeReference<List<DashboardOccupancyResponse>>() {}, this::loadOccupancy);
    }

    @Transactional(readOnly = true)
    public List<DashboardCameraResponse> cameras() {
        return cached(CAMERAS_KEY, new TypeReference<List<DashboardCameraResponse>>() {}, this::loadCameras);
    }

    @Transactional(readOnly = true)
    public List<DashboardAlertResponse> alerts(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        String key = ALERTS_KEY + ":" + safeLimit;
        return cached(key, new TypeReference<List<DashboardAlertResponse>>() {}, () -> loadAlerts(safeLimit));
    }

    private DashboardSummaryResponse loadSummary() {
        ZoneId zone = ZoneId.of(properties.zoneId());
        OffsetDateTime from = LocalDate.now(clock.withZone(zone)).atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime to = from.plusDays(1);

        long pending = gateOperationRepository.countByStatus(GateOperationStatus.PENDING);
        long exhausted = gateOperationRepository.countByStatus(GateOperationStatus.EXHAUSTED);
        return new DashboardSummaryResponse(
                apartmentRepository.countByActiveTrue(),
                residentRepository.countByActiveTrue(),
                vehicleRepository.countByActiveTrue(),
                vehicleRepository.countByActiveTrueAndInsideGarageTrue(),
                vehicleRepository.countByActiveTrueAndInsideGarageFalse(),
                parkingSpaceRepository.countByActiveTrue(),
                parkingSpaceRepository.countByActiveTrueAndStatus(ParkingSpaceStatus.AVAILABLE),
                parkingSpaceRepository.countByActiveTrueAndStatus(ParkingSpaceStatus.OCCUPIED),
                parkingSpaceRepository.countByActiveTrueAndStatus(ParkingSpaceStatus.BLOCKED),
                movementRepository.countBetween(from, to),
                movementRepository.countByDirectionBetween(from, to, MovementDirection.ENTRY),
                movementRepository.countByDirectionBetween(from, to, MovementDirection.EXIT),
                movementRepository.countByStatusBetween(from, to, MovementStatus.AUTHORIZED),
                movementRepository.countByStatusBetween(from, to, MovementStatus.DENIED),
                pending,
                exhausted,
                OffsetDateTime.now(clock)
        );
    }

    private List<DashboardOccupancyResponse> loadOccupancy() {
        List<ParkingSpace> spaces = parkingSpaceRepository.findAllByOrderByNumberAsc();
        Map<Long, Vehicle> vehicleByParkingSpaceId = new HashMap<>();
        for (Vehicle vehicle : vehicleRepository.findAllWithDetails()) {
            if (vehicle.getParkingSpace() != null) {
                vehicleByParkingSpaceId.put(vehicle.getParkingSpace().getId(), vehicle);
            }
        }

        return spaces.stream().map(space -> {
            Vehicle vehicle = vehicleByParkingSpaceId.get(space.getId());
            return new DashboardOccupancyResponse(
                    space.getId(),
                    space.getNumber(),
                    space.getApartment() == null ? null : space.getApartment().getId(),
                    space.getApartment() == null ? null : space.getApartment().getNumber(),
                    space.getType(),
                    space.getStatus(),
                    space.isActive(),
                    vehicle == null ? null : vehicle.getPlate(),
                    vehicle != null && vehicle.isInsideGarage()
            );
        }).toList();
    }

    private List<DashboardCameraResponse> loadCameras() {
        return cameraRepository.findAllByOrderByNameAsc().stream()
                .map(this::toCameraResponse)
                .toList();
    }

    private DashboardCameraResponse toCameraResponse(Camera camera) {
        Optional<HikvisionEvent> latest = hikvisionEventRepository.findTop1ByCameraIdOrderByOccurredAtDesc(camera.getId());
        return new DashboardCameraResponse(
                camera.getId(),
                camera.getName(),
                camera.getIpAddress(),
                camera.getDirection(),
                camera.getVendor(),
                camera.getModel(),
                camera.isEnabled(),
                latest.map(HikvisionEvent::getOccurredAt).orElse(null),
                latest.map(HikvisionEvent::getPlate).orElse(null)
        );
    }

    private List<DashboardAlertResponse> loadAlerts(int limit) {
        List<DashboardAlertResponse> alerts = new ArrayList<>();
        for (Movement movement : movementRepository.findTop20ByStatusOrderByEventTimeDesc(MovementStatus.DENIED)) {
            AccessDecision decision = movement.getAccessDecision();
            alerts.add(new DashboardAlertResponse(
                    "MOVEMENT-" + movement.getId(),
                    decision.name(),
                    severityFor(decision),
                    titleFor(decision),
                    "Placa " + movement.getPlate() + " — " + decision.name(),
                    movement.getEventTime(),
                    movement.getId(),
                    null
            ));
        }
        for (GateOperation operation : gateOperationRepository.findTop10ByStatusOrderByCreatedAtDesc(GateOperationStatus.EXHAUSTED)) {
            alerts.add(new DashboardAlertResponse(
                    "GATE-" + operation.getId(),
                    "GATE_EXHAUSTED",
                    "CRITICAL",
                    "Falha ao acionar cancela",
                    "Operação " + operation.getId() + " excedeu o limite de tentativas",
                    operation.getCreatedAt(),
                    operation.getMovement().getId(),
                    operation.getId()
            ));
        }
        return alerts.stream()
                .sorted(Comparator.comparing(DashboardAlertResponse::occurredAt).reversed())
                .limit(limit)
                .toList();
    }

    private String severityFor(AccessDecision decision) {
        return switch (decision) {
            case UNKNOWN_VEHICLE, VEHICLE_INACTIVE, RESIDENT_INACTIVE -> "HIGH";
            case LOW_CONFIDENCE -> "MEDIUM";
            default -> "HIGH";
        };
    }

    private String titleFor(AccessDecision decision) {
        return switch (decision) {
            case UNKNOWN_VEHICLE -> "Veículo desconhecido";
            case LOW_CONFIDENCE -> "Leitura com baixa confiança";
            case VEHICLE_INACTIVE -> "Veículo inativo";
            case RESIDENT_INACTIVE -> "Morador inativo";
            case NO_PARKING_ASSIGNED -> "Veículo sem vaga atribuída";
            case PARKING_SPACE_INACTIVE -> "Vaga inativa";
            case PARKING_SPACE_BLOCKED -> "Vaga bloqueada";
            case PARKING_SPACE_OCCUPIED -> "Vaga ocupada";
            case DUPLICATE_ENTRY -> "Entrada duplicada";
            case EXIT_WITHOUT_ENTRY -> "Saída sem entrada";
            default -> "Acesso negado";
        };
    }

    private <T> T cached(String key, Class<T> type, java.util.function.Supplier<T> loader) {
        String value = read(key);
        if (value != null) {
            try {
                return objectMapper.readValue(value, type);
            } catch (Exception ignored) {
            }
        }
        T fresh = loader.get();
        write(key, fresh);
        return fresh;
    }

    private <T> T cached(String key, TypeReference<T> type, java.util.function.Supplier<T> loader) {
        String value = read(key);
        if (value != null) {
            try {
                return objectMapper.readValue(value, type);
            } catch (Exception ignored) {
            }
        }
        T fresh = loader.get();
        write(key, fresh);
        return fresh;
    }

    private String read(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private void write(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), properties.cacheTtl());
        } catch (Exception ignored) {
            // Dashboard remains available when Redis is unavailable.
        }
    }
}
