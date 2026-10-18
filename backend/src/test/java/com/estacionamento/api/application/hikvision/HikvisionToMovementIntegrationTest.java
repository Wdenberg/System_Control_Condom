package com.estacionamento.api.application.hikvision;

import com.estacionamento.api.application.apartment.ApartmentService;
import com.estacionamento.api.application.parking.ParkingSpaceService;
import com.estacionamento.api.application.vehicle.VehicleService;
import com.estacionamento.api.domain.movement.MovementRepository;
import com.estacionamento.api.domain.parking.ParkingSpaceRepository;
import com.estacionamento.api.domain.parking.ParkingSpaceStatus;
import com.estacionamento.api.domain.vehicle.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class HikvisionToMovementIntegrationTest {

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
        registry.add("app.hikvision.image-storage-path", () -> "build/test-hikvision-movement-images");
    }

    @Autowired com.estacionamento.api.domain.gate.GateOperationRepository gateOperations;
    @Autowired HikvisionEventService hikvisionEventService;
    @Autowired ApartmentService apartmentService;
    @Autowired ParkingSpaceService parkingSpaceService;
    @Autowired VehicleService vehicleService;
    @Autowired MovementRepository movementRepository;
    @Autowired VehicleRepository vehicleRepository;
    @Autowired ParkingSpaceRepository parkingSpaceRepository;

    @BeforeEach
    void clean() {
        gateOperations.deleteAll();
        movementRepository.deleteAll();
        vehicleRepository.deleteAll();
        parkingSpaceRepository.deleteAll();
    }

    @Test
    void shouldTurnRealHikvisionEntryXmlIntoAuthorizedMovement() throws Exception {
        var apartment = apartmentService.create("601");
        var resident = apartmentService.createResident(apartment.getId(), "Maria LPR");
        var space = parkingSpaceService.create("601", apartment.getId(), com.estacionamento.api.domain.parking.ParkingSpaceType.ASSIGNED);
        vehicleService.create("SOO3H61", "Toyota", "Corolla", "White", java.util.Set.of(resident.getId()), space.getId());

        String xml = Files.readString(Path.of("src/test/resources/hikvision/anpr-entry.xml"));
        var response = hikvisionEventService.process(xml, "192.168.15.43", java.util.List.of());

        assertThat(response.accessDecision()).hasToString("AUTHORIZED");
        assertThat(response.movementId()).isNotNull();
        assertThat(vehicleRepository.findByPlateWithDetails("SOO3H61").orElseThrow().isInsideGarage()).isFalse();
        assertThat(parkingSpaceRepository.findWithApartmentById(space.getId()).orElseThrow().getStatus())
                .isEqualTo(ParkingSpaceStatus.AVAILABLE);
    }
}
