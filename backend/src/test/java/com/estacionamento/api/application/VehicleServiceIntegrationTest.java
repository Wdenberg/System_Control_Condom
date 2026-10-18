package com.estacionamento.api.application;

import com.estacionamento.api.domain.role.RoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class VehicleServiceIntegrationTest {

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
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("app.security.jwt.secret-base64", () -> JWT_SECRET);
        registry.add("app.initial-admin.enabled", () -> "false");
    }

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    com.estacionamento.api.application.apartment.ApartmentService apartmentService;

    @Autowired
    com.estacionamento.api.application.parking.ParkingSpaceService parkingService;

    @Autowired
    com.estacionamento.api.application.vehicle.VehicleService vehicleService;

    @Test
    void shouldCreateVehicleLinkedToResidentApartmentAndParkingSpace() {
        var apartment = apartmentService.create("301");
        var resident = apartmentService.createResident(apartment.getId(), "Maria Silva");
        var space = parkingService.create("227", apartment.getId(), com.estacionamento.api.domain.parking.ParkingSpaceType.ASSIGNED);

        var vehicle = vehicleService.create("SOO-3H61", "Toyota", "Corolla", "White",
                java.util.Set.of(resident.getId()), space.getId());

        var loaded = vehicleService.findByPlate("soo3h61");
        assertThat(loaded.getPlate()).isEqualTo("SOO3H61");
        assertThat(loaded.getParkingSpace().getNumber()).isEqualTo("227");
        assertThat(loaded.getResidents()).extracting("fullName").contains("Maria Silva");
    }

    @Test
    void shouldRejectDuplicatePlate() {
        var apartment = apartmentService.create("302");
        var resident = apartmentService.createResident(apartment.getId(), "Carlos Silva");
        vehicleService.create("ABC1234", null, null, null, java.util.Set.of(resident.getId()), null);

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                vehicleService.create("ABC-1234", null, null, null, java.util.Set.of(resident.getId()), null))
                .isInstanceOf(com.estacionamento.api.application.shared.DuplicateResourceException.class);
    }

    @Test
    void shouldRejectResidentsFromDifferentApartments() {
        var apartment1 = apartmentService.create("303");
        var apartment2 = apartmentService.create("304");
        var resident1 = apartmentService.createResident(apartment1.getId(), "A");
        var resident2 = apartmentService.createResident(apartment2.getId(), "B");

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                vehicleService.create("DEF5678", null, null, null,
                        java.util.Set.of(resident1.getId(), resident2.getId()), null))
                .isInstanceOf(com.estacionamento.api.application.shared.InvalidAssignmentException.class);
    }

    @Test
    void shouldRejectParkingSpaceFromDifferentApartment() {
        var apartment1 = apartmentService.create("305");
        var apartment2 = apartmentService.create("306");
        var resident = apartmentService.createResident(apartment1.getId(), "Daniel");
        var space = parkingService.create("228", apartment2.getId(), com.estacionamento.api.domain.parking.ParkingSpaceType.ASSIGNED);

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                vehicleService.create("GHI9012", null, null, null, java.util.Set.of(resident.getId()), space.getId()))
                .isInstanceOf(com.estacionamento.api.application.shared.InvalidAssignmentException.class);
    }
    @Test
    void shouldRejectTwoActiveVehiclesOnSameParkingSpace() {
        var apartment = apartmentService.create("307");
        var resident1 = apartmentService.createResident(apartment.getId(), "Eduardo");
        var resident2 = apartmentService.createResident(apartment.getId(), "Fernanda");
        var space = parkingService.create("229", apartment.getId(), com.estacionamento.api.domain.parking.ParkingSpaceType.ASSIGNED);

        vehicleService.create("JKL3456", null, null, null, java.util.Set.of(resident1.getId()), space.getId());

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                vehicleService.create("MNO7890", null, null, null, java.util.Set.of(resident2.getId()), space.getId()))
                .isInstanceOf(com.estacionamento.api.application.shared.InvalidAssignmentException.class);
    }

    @Test
    void shouldRejectVisitorParkingSpaceAsPermanentVehicleAssignment() {
        var apartment = apartmentService.create("308");
        var resident = apartmentService.createResident(apartment.getId(), "Gabriel");
        var visitorSpace = parkingService.create("V01", null, com.estacionamento.api.domain.parking.ParkingSpaceType.VISITOR);

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                vehicleService.create("PQR1234", null, null, null, java.util.Set.of(resident.getId()), visitorSpace.getId()))
                .isInstanceOf(com.estacionamento.api.application.shared.InvalidAssignmentException.class);
    }

}
