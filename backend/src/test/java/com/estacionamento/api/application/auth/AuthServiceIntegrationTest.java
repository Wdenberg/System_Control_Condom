package com.estacionamento.api.application.auth;

import com.estacionamento.api.domain.role.Role;
import com.estacionamento.api.domain.role.RoleCode;
import com.estacionamento.api.domain.role.RoleRepository;
import com.estacionamento.api.domain.user.User;
import com.estacionamento.api.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
class AuthServiceIntegrationTest {

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
        registry.add("app.security.jwt.secret-base64", () -> "VGhpc0lzQVN0cm9uZ0xvY2FsVGVzdFNLb3lUb1VzZUZvclRlc3RpbmdPbkx5IQ==");
        registry.add("app.initial-admin.enabled", () -> "false");
    }

    @Autowired com.estacionamento.api.domain.audit.AuditLogRepository auditLogs;

    @Autowired
    AuthService authService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanUsers() {
        auditLogs.deleteAll(); userRepository.deleteAll();
    }

    @Test
    void shouldAuthenticateAndIssueAccessToken() {
        Role role = roleRepository.findByCode(RoleCode.ADMIN).orElseThrow();
        userRepository.save(new User("admin", passwordEncoder.encode("StrongPassword123!"), "Administrador", role));

        var result = authService.authenticate("admin", "StrongPassword123!");

        assertThat(result.user().getUsername()).isEqualTo("admin");
        assertThat(result.user().getRole()).isEqualTo(RoleCode.ADMIN);
        assertThat(result.token().value()).isNotBlank();
        assertThat(result.token().expiresInSeconds()).isGreaterThan(0);
    }

    @Test
    void shouldRejectInvalidPassword() {
        Role role = roleRepository.findByCode(RoleCode.ADMIN).orElseThrow();
        userRepository.save(new User("admin", passwordEncoder.encode("StrongPassword123!"), "Administrador", role));

        assertThatThrownBy(() -> authService.authenticate("admin", "wrong-password"))
                .isInstanceOf(BadCredentialsException.class);
    }
}
