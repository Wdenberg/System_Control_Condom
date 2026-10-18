package com.estacionamento.api.application.hikvision;

import com.estacionamento.api.domain.camera.CameraRepository;
import com.estacionamento.api.domain.hikvision.HikvisionEventRepository;
import com.estacionamento.api.domain.hikvision.HikvisionPictureRepository;
import com.estacionamento.api.infrastructure.hikvision.HikvisionImageStorage;
import com.estacionamento.api.infrastructure.hikvision.HikvisionXmlParser;
import com.estacionamento.api.infrastructure.hikvision.PlateNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class HikvisionEventServiceIntegrationTest {

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
        registry.add("app.security.jwt.secret-base64", () -> "VGhpc0lzQVN0cm9uZ0xvY2FsVGVzdFNLb3lUb1VzZUZvclRlc3RpbmdPbkx5IQ==");
        registry.add("app.initial-admin.enabled", () -> "false");
        registry.add("app.hikvision.image-storage-path", () -> "build/test-hikvision-images");
        registry.add("app.hikvision.webhook-token", () -> "");
    }

    @Autowired com.estacionamento.api.domain.gate.GateOperationRepository gateOperations;
    @Autowired HikvisionEventService service;
    @Autowired HikvisionEventRepository eventRepository;
    @Autowired HikvisionPictureRepository pictureRepository;
    @Autowired CameraRepository cameraRepository;
    @Autowired com.estacionamento.api.domain.movement.MovementRepository movementRepository;
    @Autowired StringRedisTemplate redisTemplate;

    @BeforeEach
    void clean() {
        redisTemplate.execute((RedisCallback<Object>) connection -> { connection.serverCommands().flushDb(); return null; });
        gateOperations.deleteAll();
        movementRepository.deleteAll();
        pictureRepository.deleteAll();
        eventRepository.deleteAll();
    }

    @Test
    void shouldPersistEntryEventAndBeIdempotent() throws Exception {
        String xml = Files.readString(Path.of("src/test/resources/hikvision/anpr-entry.xml"));

        var first = service.process(xml, "192.168.15.43", List.of());
        var second = service.process(xml, "192.168.15.43", List.of());

        assertThat(first.status()).isEqualTo("RECEIVED");
        assertThat(first.plate()).isEqualTo("SOO3H61");
        assertThat(first.cameraName()).isEqualTo("CAM-ENTRADA");
        assertThat(first.cameraDirection()).hasToString("ENTRY");
        assertThat(first.accessDecision()).hasToString("UNKNOWN_VEHICLE");
        assertThat(first.movementId()).isNotNull();
        assertThat(second.status()).isEqualTo("DUPLICATE");
        assertThat(eventRepository.count()).isEqualTo(1);
        assertThat(pictureRepository.count()).isEqualTo(3);
    }

    @Test
    void shouldApplyThirtySecondCooldownToASecondRecognitionOfTheSamePlate() throws Exception {
        String xml = Files.readString(Path.of("src/test/resources/hikvision/anpr-entry.xml"));
        var first = service.process(xml, "192.168.15.43", List.of());

        String secondXml = xml
                .replace("SOO3H61", "SOO3H61")
                .replace("2026091114341198100oYBxL05zckBgOr6Ao4dUwU5lxxn8PtPR3YBdDZ8yQCP2", "2026091312000198100oYBxL05zckBgOr6Ao4dUwU5lxxn8PtPR3YBdDZ8yQCP8");
        var second = service.process(secondXml, "192.168.15.43", List.of());

        assertThat(first.status()).isEqualTo("RECEIVED");
        assertThat(second.status()).isEqualTo("COOLDOWN");
        assertThat(eventRepository.count()).isEqualTo(2);
    }

    @Test
    void shouldHaveTheTwoSeededCameras() {
        assertThat(cameraRepository.findByIpAddressAndEnabledTrue("192.168.15.43")).isPresent();
        assertThat(cameraRepository.findByIpAddressAndEnabledTrue("192.168.15.44")).isPresent();
    }
}
