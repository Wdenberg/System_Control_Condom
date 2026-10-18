package com.estacionamento.api.application;

import com.estacionamento.api.domain.role.RoleRepository;
import com.estacionamento.api.domain.apartment.ApartmentRepository;
import com.estacionamento.api.domain.resident.ResidentRepository;
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

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class ApartmentServiceIntegrationTest {

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
    com.estacionamento.api.application.apartment.ApartmentService service;

    @Autowired
    com.estacionamento.api.domain.apartment.ApartmentRepository apartmentRepository;

    @Autowired
    ResidentRepository residentRepository;

    @BeforeEach
    void cleanDatabase() {
        residentRepository.deleteAll();
        apartmentRepository.deleteAll();
    }

    @Test
    void shouldCreateApartmentAndResident() {
        var apartment = service.create("101");
        var resident = service.createResident(apartment.getId(), "João da Silva");

        assertThat(apartment.getNumber()).isEqualTo("101");
        assertThat(resident.getApartment().getId()).isEqualTo(apartment.getId());
        assertThat(service.listResidents(apartment.getId())).extracting("fullName")
                .containsExactly("João da Silva");
    }

    @Test
    void shouldRejectDuplicateApartmentNumber() {
        service.create("101");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.create("101"))
                .isInstanceOf(com.estacionamento.api.application.shared.DuplicateResourceException.class);
    }
}
