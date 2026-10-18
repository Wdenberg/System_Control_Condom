package com.estacionamento.api.web;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.estacionamento.api.domain.role.Role;
import com.estacionamento.api.domain.role.RoleCode;
import com.estacionamento.api.domain.role.RoleRepository;
import com.estacionamento.api.domain.user.User;
import com.estacionamento.api.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class DomainCrudAuthorizationIntegrationTest {

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
    }

    @Autowired com.estacionamento.api.domain.audit.AuditLogRepository auditLogs;

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ObjectMapper objectMapper;

    @BeforeEach
    void cleanUsers() {
        auditLogs.deleteAll(); userRepository.deleteAll();
    }

    @Test
    void viewerCanReadButCannotWriteApartment() throws Exception {
        String token = createUserAndLogin("viewer", RoleCode.VIEWER);

        mockMvc.perform(get("/api/v1/apartments").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/apartments")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"number\":\"101\",\"active\":true}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void attendantCannotWriteVehicle() throws Exception {
        String token = createUserAndLogin("attendant", RoleCode.ATTENDANT);

        mockMvc.perform(post("/api/v1/vehicles")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"plate\":\"ABC1234\",\"residentIds\":[1],\"active\":true}"))
                .andExpect(status().isForbidden());
    }

    private String createUserAndLogin(String username, RoleCode roleCode) throws Exception {
        Role role = roleRepository.findByCode(roleCode).orElseThrow();
        userRepository.save(new User(username, passwordEncoder.encode("StrongPassword123!"), username, role));

        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"" + username + "\",\"password\":\"StrongPassword123!\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        return json.get("accessToken").asText();
    }
}
