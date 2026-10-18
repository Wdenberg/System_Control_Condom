package com.estacionamento.api.application.report;

import com.estacionamento.api.config.properties.DashboardProperties;
import com.estacionamento.api.domain.gate.GateOperation;
import com.estacionamento.api.domain.gate.GateOperationRepository;
import com.estacionamento.api.domain.gate.GateOperationStatus;
import com.estacionamento.api.domain.movement.AccessDecision;
import com.estacionamento.api.domain.movement.Movement;
import com.estacionamento.api.domain.movement.MovementDirection;
import com.estacionamento.api.domain.movement.MovementRepository;
import com.estacionamento.api.domain.movement.MovementSource;
import com.estacionamento.api.domain.movement.MovementStatus;
import com.estacionamento.api.domain.parking.ParkingSpace;
import com.estacionamento.api.domain.parking.ParkingSpaceRepository;
import com.estacionamento.api.domain.vehicle.Vehicle;
import com.estacionamento.api.domain.vehicle.VehicleRepository;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_CSV_ROWS = 50_000;
    private static final int MAX_PERIOD_DAYS = 366;

    private final MovementRepository movementRepository;
    private final ParkingSpaceRepository parkingSpaceRepository;
    private final VehicleRepository vehicleRepository;
    private final GateOperationRepository gateOperationRepository;
    private final DashboardProperties dashboardProperties;
    private final Clock clock;

    public ReportService(MovementRepository movementRepository,
                         ParkingSpaceRepository parkingSpaceRepository,
                         VehicleRepository vehicleRepository,
                         GateOperationRepository gateOperationRepository,
                         DashboardProperties dashboardProperties,
                         Clock clock) {
        this.movementRepository = movementRepository;
        this.parkingSpaceRepository = parkingSpaceRepository;
        this.vehicleRepository = vehicleRepository;
        this.gateOperationRepository = gateOperationRepository;
        this.dashboardProperties = dashboardProperties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Page<AccessReportRow> access(AccessReportFilter filter, Pageable pageable) {
        AccessReportFilter normalized = normalizeFilter(filter);
        Pageable safePageable = PageRequest.of(
                pageable.getPageNumber(),
                Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "eventTime")
        );
        return movementRepository.findAll(buildMovementSpecification(normalized), safePageable)
                .map(AccessReportRow::from);
    }

    @Transactional(readOnly = true)
    public AccessReportSummary accessSummary(AccessReportFilter filter) {
        AccessReportFilter normalized = normalizeFilter(filter);
        Specification<Movement> base = buildMovementSpecification(normalized);
        long total = movementRepository.count(base);
        long entries = movementRepository.count(base.and(hasDirection(MovementDirection.ENTRY)));
        long exits = movementRepository.count(base.and(hasDirection(MovementDirection.EXIT)));
        long authorized = movementRepository.count(base.and(hasStatus(MovementStatus.AUTHORIZED)));
        long denied = movementRepository.count(base.and(hasStatus(MovementStatus.DENIED)));
        long overridden = movementRepository.count(base.and(hasStatus(MovementStatus.OVERRIDDEN)));

        Map<String, Long> decisions = new LinkedHashMap<>();
        for (AccessDecision decision : AccessDecision.values()) {
            long count = movementRepository.count(base.and(hasDecision(decision)));
            if (count > 0) {
                decisions.put(decision.name(), count);
            }
        }
        return new AccessReportSummary(normalized.from(), normalized.to(), total, entries, exits, authorized, denied, overridden, decisions);
    }

    @Transactional(readOnly = true)
    public List<AccessReportRow> accessForCsv(AccessReportFilter filter) {
        AccessReportFilter normalized = normalizeFilter(filter);
        Pageable page = PageRequest.of(0, MAX_CSV_ROWS, Sort.by(Sort.Direction.DESC, "eventTime"));
        return movementRepository.findAll(buildMovementSpecification(normalized), page)
                .getContent().stream().map(AccessReportRow::from).toList();
    }

    @Transactional(readOnly = true)
    public List<OccupancyReportRow> occupancy() {
        List<Vehicle> vehicles = vehicleRepository.findAllWithDetails();
        Map<Long, Vehicle> vehicleBySpaceId = new LinkedHashMap<>();
        vehicles.stream()
                .filter(Vehicle::isActive)
                .filter(v -> v.getParkingSpace() != null)
                .sorted(Comparator.comparing(Vehicle::getPlate))
                .forEach(v -> vehicleBySpaceId.putIfAbsent(v.getParkingSpace().getId(), v));

        List<OccupancyReportRow> rows = new ArrayList<>();
        for (ParkingSpace space : parkingSpaceRepository.findAllByOrderByNumberAsc()) {
            Vehicle vehicle = vehicleBySpaceId.get(space.getId());
            rows.add(new OccupancyReportRow(
                    space.getId(),
                    space.getNumber(),
                    space.getApartment() == null ? null : space.getApartment().getId(),
                    space.getApartment() == null ? null : space.getApartment().getNumber(),
                    space.getType().name(),
                    space.getStatus().name(),
                    space.isActive(),
                    vehicle == null ? null : vehicle.getPlate(),
                    vehicle != null && vehicle.isInsideGarage()
            ));
        }
        return rows;
    }

    @Transactional(readOnly = true)
    public Page<GateReportRow> gates(OffsetDateTime from, OffsetDateTime to,
                                     GateOperationStatus status, Pageable pageable) {
        Period period = normalizePeriod(from, to);
        Pageable safe = PageRequest.of(
                pageable.getPageNumber(),
                Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        Specification<GateOperation> specification = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), period.from()));
            predicates.add(cb.lessThan(root.get("createdAt"), period.to()));
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        return gateOperationRepository.findAll(specification, safe).map(GateReportRow::from);
    }

    public String csv(List<AccessReportRow> rows) {
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("ID;Data/Hora;Placa;Direção;Origem;Status;Decisão;Confiança;Morador;Apartamento;Vaga;Câmera;IP Câmera\n");
        for (AccessReportRow row : rows) {
            csv.append(csv(row.movementId())).append(';')
                    .append(csv(row.eventTime())).append(';')
                    .append(csv(row.plate())).append(';')
                    .append(csv(row.direction())).append(';')
                    .append(csv(row.source())).append(';')
                    .append(csv(row.status())).append(';')
                    .append(csv(row.decision())).append(';')
                    .append(csv(row.confidenceLevel())).append(';')
                    .append(csv(row.vehicleOwner())).append(';')
                    .append(csv(row.apartmentNumber())).append(';')
                    .append(csv(row.parkingSpaceNumber())).append(';')
                    .append(csv(row.cameraName())).append(';')
                    .append(csv(row.cameraIp())).append('\n');
        }
        return csv.toString();
    }

    private String csv(Object value) {
        if (value == null) return "\"\"";
        return "\"" + String.valueOf(value).replace("\"", "\"\"") + "\"";
    }

    private Specification<Movement> buildMovementSpecification(AccessReportFilter filter) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.greaterThanOrEqualTo(root.get("eventTime"), filter.from()));
            predicates.add(cb.lessThan(root.get("eventTime"), filter.to()));
            if (filter.plate() != null && !filter.plate().isBlank()) {
                predicates.add(cb.like(cb.upper(root.get("plate")), "%" + filter.plate().toUpperCase() + "%"));
            }
            if (filter.direction() != null) predicates.add(cb.equal(root.get("direction"), filter.direction()));
            if (filter.status() != null) predicates.add(cb.equal(root.get("status"), filter.status()));
            if (filter.decision() != null) predicates.add(cb.equal(root.get("accessDecision"), filter.decision()));
            if (filter.source() != null) predicates.add(cb.equal(root.get("source"), filter.source()));
            if (filter.cameraId() != null) {
                var camera = root.join("camera", JoinType.LEFT);
                predicates.add(cb.equal(camera.get("id"), filter.cameraId()));
            }
            if (filter.apartmentId() != null) {
                var parkingSpace = root.join("parkingSpace", JoinType.LEFT);
                var apartment = parkingSpace.join("apartment", JoinType.LEFT);
                predicates.add(cb.equal(apartment.get("id"), filter.apartmentId()));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private Specification<Movement> hasDirection(MovementDirection direction) {
        return (root, query, cb) -> cb.equal(root.get("direction"), direction);
    }

    private Specification<Movement> hasStatus(MovementStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    private Specification<Movement> hasDecision(AccessDecision decision) {
        return (root, query, cb) -> cb.equal(root.get("accessDecision"), decision);
    }

    private AccessReportFilter normalizeFilter(AccessReportFilter filter) {
        Period period = normalizePeriod(filter.from(), filter.to());
        String plate = filter.plate() == null ? null : filter.plate().replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        return new AccessReportFilter(period.from(), period.to(), plate, filter.direction(), filter.status(),
                filter.decision(), filter.source(), filter.cameraId(), filter.apartmentId());
    }

    private Period normalizePeriod(OffsetDateTime from, OffsetDateTime to) {
        ZoneId zone = ZoneId.of(dashboardProperties.zoneId());
        OffsetDateTime defaultFrom = LocalDate.now(clock.withZone(zone)).atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime normalizedFrom = from == null ? defaultFrom : from;
        OffsetDateTime normalizedTo = to == null ? normalizedFrom.plusDays(1) : to;
        if (!normalizedFrom.isBefore(normalizedTo)) {
            throw new IllegalArgumentException("Report start must be before report end");
        }
        if (normalizedTo.isAfter(normalizedFrom.plusDays(MAX_PERIOD_DAYS))) {
            throw new IllegalArgumentException("Report period cannot exceed 366 days");
        }
        return new Period(normalizedFrom, normalizedTo);
    }

    private record Period(OffsetDateTime from, OffsetDateTime to) { }
}
