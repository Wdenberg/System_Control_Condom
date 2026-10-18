package com.estacionamento.api.application;

import com.estacionamento.api.application.movement.MovementService;
import com.estacionamento.api.domain.camera.CameraRepository;
import com.estacionamento.api.domain.gate.GateOperationRepository;
import com.estacionamento.api.domain.movement.AccessDecision;
import com.estacionamento.api.domain.movement.MovementDirection;
import com.estacionamento.api.domain.parking.ParkingSpaceRepository;
import com.estacionamento.api.domain.role.RoleRepository;
import com.estacionamento.api.domain.resident.ResidentRepository;
import com.estacionamento.api.domain.vehicle.VehicleRepository;
import com.estacionamento.api.application.apartment.ApartmentService;
import com.estacionamento.api.application.parking.ParkingSpaceService;
import com.estacionamento.api.application.vehicle.VehicleService;
import com.estacionamento.api.application.audit.AuditService;
import com.estacionamento.api.domain.user.User;
import com.estacionamento.api.domain.user.UserRepository;
import com.estacionamento.api.domain.role.Role;
import com.estacionamento.api.domain.role.RoleCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class MovementServiceIntegrationTest {

    private static final String JWT_SECRET = "VGhpc0lzQVN0cm9uZ0xvY2FsVGVzdFNLb3lUb1VzZUZvclRlc3RpbmdPbkx5IQ==";

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("garage_control")
            .withUsername("garage")
            .withPassword("test-password");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("app.security.jwt.secret-base64", () -> JWT_SECRET);
        registry.add("app.initial-admin.enabled", () -> "false");
        registry.add("app.hikvision.webhook-token", () -> "");
    }

    @Autowired MovementService movementService;
    @Autowired ApartmentService apartmentService;
    @Autowired ParkingSpaceService parkingSpaceService;
    @Autowired VehicleService vehicleService;
    @Autowired VehicleRepository vehicleRepository;
    @Autowired ParkingSpaceRepository parkingSpaceRepository;
    @Autowired CameraRepository cameraRepository;
    @Autowired com.estacionamento.api.domain.movement.MovementRepository movementRepository;
    @Autowired GateOperationRepository gateOperationRepository;
    @Autowired ResidentRepository residentRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private Long observer() {
        return userRepository.save(new User("observer-" + System.nanoTime(), passwordEncoder.encode("ObserverPassword123!"), "Observer", roleRepository.findByCode(RoleCode.ADMIN).orElseThrow())).getId();
    }

    @BeforeEach
    void cleanMovements() {
        gateOperationRepository.deleteAll();
        movementRepository.deleteAll();
        vehicleRepository.deleteAll();
        residentRepository.deleteAll();
        parkingSpaceRepository.deleteAll();
    }

    @Test
    void shouldAuthorizeEntryMarkVehicleInsideAndOccupyAssignedSpace() {
        var apartment = apartmentService.create("501");
        var resident = apartmentService.createResident(apartment.getId(), "Maria Silva");
        var space = parkingSpaceService.create("501", apartment.getId(), com.estacionamento.api.domain.parking.ParkingSpaceType.ASSIGNED);
        vehicleService.create("ABC1234", "Toyota", "Corolla", "White", Set.of(resident.getId()), space.getId());
        var camera = cameraRepository.findByIpAddressAndEnabledTrue("192.168.15.43").orElseThrow();

        var result = movementService.processAutomatic(null, camera.getId(), "ABC-1234", 98, OffsetDateTime.now());

        assertThat(result.status()).hasToString("AUTHORIZED");
        assertThat(result.decision()).isEqualTo(AccessDecision.AUTHORIZED);
        assertThat(vehicleRepository.findByPlateWithDetails("ABC1234").orElseThrow().isInsideGarage()).isFalse();
        movementService.confirmPassage(result.movementId(), true, "Passage observed by operator", observer());
        var vehicle = vehicleRepository.findByPlateWithDetails("ABC1234").orElseThrow();
        var loadedSpace = parkingSpaceRepository.findWithApartmentById(space.getId()).orElseThrow();
        assertThat(vehicle.isInsideGarage()).isTrue();
        assertThat(loadedSpace.getStatus()).isEqualTo(com.estacionamento.api.domain.parking.ParkingSpaceStatus.OCCUPIED);
    }

    @Test
    void shouldRejectDuplicateEntryAndAuthorizeExit() {
        var apartment = apartmentService.create("502");
        var resident = apartmentService.createResident(apartment.getId(), "Joao Silva");
        var space = parkingSpaceService.create("502", apartment.getId(), com.estacionamento.api.domain.parking.ParkingSpaceType.ASSIGNED);
        vehicleService.create("DEF5678", null, null, null, Set.of(resident.getId()), space.getId());
        var entryCamera = cameraRepository.findByIpAddressAndEnabledTrue("192.168.15.43").orElseThrow();
        var exitCamera = cameraRepository.findByIpAddressAndEnabledTrue("192.168.15.44").orElseThrow();

        var firstEntry = movementService.processAutomatic(null, entryCamera.getId(), "DEF5678", 99, OffsetDateTime.now());
        movementService.confirmPassage(firstEntry.movementId(), true, "Passage observed by operator", observer());
        var duplicateEntry = movementService.processAutomatic(null, entryCamera.getId(), "DEF5678", 99, OffsetDateTime.now().plusSeconds(1));
        var exit = movementService.processAutomatic(null, exitCamera.getId(), "DEF5678", 99, OffsetDateTime.now().plusSeconds(2));

        movementService.confirmPassage(exit.movementId(), true, "Exit observed by operator", observer());
        assertThat(firstEntry.decision()).isEqualTo(AccessDecision.AUTHORIZED);
        assertThat(duplicateEntry.decision()).isEqualTo(AccessDecision.DUPLICATE_ENTRY);
        assertThat(exit.decision()).isEqualTo(AccessDecision.AUTHORIZED);
        assertThat(vehicleRepository.findByPlateWithDetails("DEF5678").orElseThrow().isInsideGarage()).isFalse();
        assertThat(parkingSpaceRepository.findWithApartmentById(space.getId()).orElseThrow().getStatus())
                .isEqualTo(com.estacionamento.api.domain.parking.ParkingSpaceStatus.AVAILABLE);
    }

    @Test
    void shouldRejectUnknownAndLowConfidenceEvents() {
        var unknown = movementService.processAutomatic(null,
                cameraRepository.findByIpAddressAndEnabledTrue("192.168.15.43").orElseThrow().getId(),
                "ZZZ9999", 99, OffsetDateTime.now());
        var lowConfidence = movementService.processAutomatic(null,
                cameraRepository.findByIpAddressAndEnabledTrue("192.168.15.43").orElseThrow().getId(),
                "YYY8888", 79, OffsetDateTime.now());

        assertThat(unknown.decision()).isEqualTo(AccessDecision.UNKNOWN_VEHICLE);
        assertThat(lowConfidence.decision()).isEqualTo(AccessDecision.LOW_CONFIDENCE);
    }

    @Test
    void shouldCreateManualMovementAndAuditIt() {
        var apartment = apartmentService.create("503");
        var resident = apartmentService.createResident(apartment.getId(), "Ana Silva");
        var space = parkingSpaceService.create("503", apartment.getId(), com.estacionamento.api.domain.parking.ParkingSpaceType.ASSIGNED);
        vehicleService.create("JKL9012", null, null, null, Set.of(resident.getId()), space.getId());
        Role role = roleRepository.findByCode(RoleCode.ATTENDANT).orElseThrow();
        User user = userRepository.save(new User("attendant-" + System.nanoTime(), passwordEncoder.encode("AttendantPassword123!"), "Portaria", role));

        var result = movementService.createManual("JKL9012", MovementDirection.ENTRY, "Liberacao de teste", user.getId());

        assertThat(result.status()).hasToString("AUTHORIZED");
        assertThat(result.decision()).isEqualTo(AccessDecision.MANUAL_AUTHORIZED);
    }
    @Test
    void shouldAllowInactiveVehicleToExitWhenItIsInside() {
        var apartment = apartmentService.create("504");
        var resident = apartmentService.createResident(apartment.getId(), "Carlos Silva");
        var space = parkingSpaceService.create("504", apartment.getId(), com.estacionamento.api.domain.parking.ParkingSpaceType.ASSIGNED);
        vehicleService.create("MNO3456", null, null, null, Set.of(resident.getId()), space.getId());
        var entryCamera = cameraRepository.findByIpAddressAndEnabledTrue("192.168.15.43").orElseThrow();
        var exitCamera = cameraRepository.findByIpAddressAndEnabledTrue("192.168.15.44").orElseThrow();

        var entry = movementService.processAutomatic(null, entryCamera.getId(), "MNO3456", 99, OffsetDateTime.now());
        movementService.confirmPassage(entry.movementId(), true, "Passage observed by operator", observer());
        var vehicle = vehicleRepository.findByPlateWithDetails("MNO3456").orElseThrow();
        vehicle.setActive(false);
        vehicleRepository.saveAndFlush(vehicle);

        var exit = movementService.processAutomatic(null, exitCamera.getId(), "MNO3456", 99, OffsetDateTime.now().plusSeconds(1));

        assertThat(exit.decision()).isEqualTo(AccessDecision.AUTHORIZED);
        movementService.confirmPassage(exit.movementId(), true, "Exit observed by operator", observer());
        assertThat(vehicleRepository.findByPlateWithDetails("MNO3456").orElseThrow().isInsideGarage()).isFalse();
    }

}
