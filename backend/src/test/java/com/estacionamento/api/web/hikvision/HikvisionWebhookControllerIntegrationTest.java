package com.estacionamento.api.web.hikvision;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class HikvisionWebhookControllerIntegrationTest {

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
        registry.add("app.hikvision.image-storage-path", () -> "build/test-hikvision-webhook-images");
        registry.add("app.hikvision.webhook.tokens.CAM-ENTRADA", () -> "webhook-test-token-at-least-32-characters");
        registry.add("app.hikvision.enforce-remote-ip", () -> "false");
    }

    @Autowired MockMvc mockMvc;

    private String xml;

    @BeforeEach
    void loadFixture() throws Exception {
        xml = Files.readString(Path.of("src/test/resources/hikvision/anpr-entry.xml"));
    }

    @Autowired com.estacionamento.api.domain.movement.MovementRepository movements;
    @Autowired com.estacionamento.api.domain.gate.GateOperationRepository operations;
    @Test void rejectedWebhookHasNoBusinessEffects() throws Exception {
        long beforeMovements = movements.count(), beforeOperations = operations.count();
        mockMvc.perform(post("/api/v1/integrations/hikvision/events").contentType(MediaType.APPLICATION_XML)
                .header("X-Hikvision-Webhook-Token", "other-camera-token-at-least-32-characters").content(xml))
                .andExpect(status().isUnauthorized());
        org.assertj.core.api.Assertions.assertThat(movements.count()).isEqualTo(beforeMovements);
        org.assertj.core.api.Assertions.assertThat(operations.count()).isEqualTo(beforeOperations);
    }

    @Test
    void shouldAcceptXmlWebhookWithoutJwtWhenTokenIsValid() throws Exception {
        mockMvc.perform(post("/api/v1/integrations/hikvision/events")
                        .contentType(MediaType.APPLICATION_XML)
                        .header("X-Hikvision-Webhook-Token", "webhook-test-token-at-least-32-characters")
                        .content(xml))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.cameraName").value("CAM-ENTRADA"))
                .andExpect(jsonPath("$.cameraDirection").value("ENTRY"))
                .andExpect(jsonPath("$.plate").value("SOO3H61"));
    }

    @Test
    void shouldAcceptMultipartWebhookAndStoreImageParts() throws Exception {
        String secondEvent = xml
                .replace("SOO3H61", "ABC1D23")
                .replace("2026091114341198100oYBxL05zckBgOr6Ao4dUwU5lxxn8PtPR3YBdDZ8yQCP2", "2026091312000098100oYBxL05zckBgOr6Ao4dUwU5lxxn8PtPR3YBdDZ8yQCP9");

        MockMultipartFile eventXml = new MockMultipartFile(
                "anpr.xml", "anpr.xml", "text/xml", secondEvent.getBytes()
        );
        MockMultipartFile plateImage = new MockMultipartFile(
                "licensePlatePicture.jpg", "licensePlatePicture.jpg", "image/jpeg", new byte[]{1, 2, 3, 4}
        );
        MockMultipartFile vehicleImage = new MockMultipartFile(
                "vehiclePicture.jpg", "vehiclePicture.jpg", "image/jpeg", new byte[]{5, 6, 7}
        );
        MockMultipartFile detectionImage = new MockMultipartFile(
                "detectionPicture.jpg", "detectionPicture.jpg", "image/jpeg", new byte[]{8, 9}
        );

        mockMvc.perform(multipart("/api/v1/integrations/hikvision/events")
                        .file(eventXml)
                        .file(plateImage)
                        .file(vehicleImage)
                        .file(detectionImage)
                        .header("X-Hikvision-Webhook-Token", "webhook-test-token-at-least-32-characters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.plate").value("ABC1D23"))
                .andExpect(jsonPath("$.pictureCount").value(3));
    }

    @Test
    void shouldRejectWebhookWithoutTokenWhenTokenProtectionIsConfigured() throws Exception {
        mockMvc.perform(post("/api/v1/integrations/hikvision/events")
                        .contentType(MediaType.APPLICATION_XML)
                        .content(xml))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectWebhookWithInvalidToken() throws Exception {
        mockMvc.perform(post("/api/v1/integrations/hikvision/events")
                        .contentType(MediaType.APPLICATION_XML)
                        .header("X-Hikvision-Webhook-Token", "wrong")
                        .content(xml))
                .andExpect(status().isUnauthorized());
    }
}
