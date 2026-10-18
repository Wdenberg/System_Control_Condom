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
class ParkingSpaceServiceIntegrationTest {

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
    com.estacionamento.api.application.parking.ParkingSpaceService service;

    @Test
    void shouldCreateAssignedParkingSpaceForApartment() {
        var apartment = apartmentService.create("201");
        var space = service.create("127", apartment.getId(), com.estacionamento.api.domain.parking.ParkingSpaceType.ASSIGNED);

        assertThat(space.getNumber()).isEqualTo("127");
        assertThat(space.getApartment().getNumber()).isEqualTo("201");
        assertThat(space.getStatus()).isEqualTo(com.estacionamento.api.domain.parking.ParkingSpaceStatus.AVAILABLE);
    }

    @Test
    void shouldRejectAssignedSpaceWithoutApartment() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.create("128", null, com.estacionamento.api.domain.parking.ParkingSpaceType.ASSIGNED))
                .isInstanceOf(com.estacionamento.api.application.shared.InvalidAssignmentException.class);
    }
}
