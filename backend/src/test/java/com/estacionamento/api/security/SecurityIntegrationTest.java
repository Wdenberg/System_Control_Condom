package com.estacionamento.api.security;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

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

    @Autowired com.estacionamento.api.domain.audit.AuditLogRepository auditLogs;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    ObjectMapper objectMapper;

    @BeforeEach
    void cleanUsers() {
        auditLogs.deleteAll(); userRepository.deleteAll();
    }

    @Test
    void viewerShouldAccessDashboardButNotUserWrite() throws Exception {
        Role role = roleRepository.findByCode(RoleCode.VIEWER).orElseThrow();
        userRepository.save(new User("viewer", passwordEncoder.encode("ViewerPassword123!"), "Visualizador", role));

        String token = login("viewer", "ViewerPassword123!");

        mockMvc.perform(get("/api/v1/system/version")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phase").value("19-release"));

        mockMvc.perform(get("/api/v1/system/authorization/user-write")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousUserShouldNotAccessProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/system/version"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginShouldReturnUserRoleAndPermissions() throws Exception {
        Role role = roleRepository.findByCode(RoleCode.ATTENDANT).orElseThrow();
        userRepository.save(new User("portaria", passwordEncoder.encode("PortariaPassword123!"), "Portaria", role));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"username":"portaria","password":"PortariaPassword123!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").isNumber())
                .andExpect(jsonPath("$.user.username").value("portaria"))
                .andExpect(jsonPath("$.user.role").value("ATTENDANT"))
                .andExpect(jsonPath("$.user.permissions").isArray());
    }

    @Test
    void invalidPasswordShouldReturnUnauthorized() throws Exception {
        Role role = roleRepository.findByCode(RoleCode.ADMIN).orElseThrow();
        userRepository.save(new User("admin", passwordEncoder.encode("AdminPassword123!"), "Administrador", role));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"username":"admin","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void authenticatedUserShouldReadOwnProfile() throws Exception {
        Role role = roleRepository.findByCode(RoleCode.OPERATOR).orElseThrow();
        userRepository.save(new User("operator", passwordEncoder.encode("OperatorPassword123!"), "Operador", role));

        String token = login("operator", "OperatorPassword123!");

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("operator"))
                .andExpect(jsonPath("$.user.role").value("OPERATOR"));
    }

    private String login(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(body);
        return json.get("accessToken").asText();
    }
}
