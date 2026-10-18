package com.estacionamento.api.web.user;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class UserManagementIntegrationTest {
    private static final String SECRET = "VGhpc0lzQVN0cm9uZ0xvY2FsVGVzdFNLb3lUb1VzZUZvclRlc3RpbmdPbkx5IQ==";

    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine").withDatabaseName("garage_control").withUsername("garage").withPassword("test-password");
    @Container static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", POSTGRES::getJdbcUrl); r.add("spring.datasource.username", POSTGRES::getUsername); r.add("spring.datasource.password", POSTGRES::getPassword);
        r.add("spring.data.redis.host", REDIS::getHost); r.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        r.add("app.security.jwt.secret-base64", () -> SECRET); r.add("app.initial-admin.enabled", () -> "false");
    }

    @Autowired com.estacionamento.api.domain.audit.AuditLogRepository auditLogs;

    @Autowired MockMvc mvc; @Autowired UserRepository users; @Autowired RoleRepository roles; @Autowired PasswordEncoder encoder; @Autowired ObjectMapper mapper;

    @BeforeEach void clean() { auditLogs.deleteAll(); users.deleteAll(); }

    @Test void adminCanCreateAndUpdateUser() throws Exception {
        Role admin = roles.findByCode(RoleCode.ADMIN).orElseThrow();
        users.save(new User("admin", encoder.encode("AdminPassword123!"), "Admin", admin));
        String token = login("admin", "AdminPassword123!");
        String body = """
                {"username":"operator","password":"OperatorPassword123!","fullName":"Operador","role":"OPERATOR","active":true}
                """;
        mvc.perform(post("/api/v1/users").header("Authorization", "Bearer " + token).contentType("application/json").content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.username").value("operator"));
    }

    @Test void viewerCannotReadOrCreateUsers() throws Exception {
        Role role = roles.findByCode(RoleCode.VIEWER).orElseThrow();
        users.save(new User("viewer", encoder.encode("ViewerPassword123!"), "Viewer", role));
        String token = login("viewer", "ViewerPassword123!");
        mvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/users").header("Authorization", "Bearer " + token).contentType("application/json").content("{\"username\":\"newuser\",\"password\":\"Password123!\",\"fullName\":\"New User\",\"role\":\"OPERATOR\",\"active\":true}"))
                .andExpect(status().isForbidden());
    }

    @Test void cannotDeactivateLastSuperAdmin() throws Exception {
        Role role = roles.findByCode(RoleCode.SUPER_ADMIN).orElseThrow();
        User root = users.save(new User("root", encoder.encode("RootPassword123!"), "Root", role));
        String token = login("root", "RootPassword123!");
        mvc.perform(put("/api/v1/users/" + root.getId()).header("Authorization", "Bearer " + token).contentType("application/json")
                        .content("{\"fullName\":\"Root\",\"role\":\"ADMIN\",\"active\":true}"))
                .andExpect(status().isConflict());
    }


    @Test void adminCannotCreateSuperAdmin() throws Exception {
        Role role = roles.findByCode(RoleCode.ADMIN).orElseThrow();
        users.save(new User("admin", encoder.encode("AdminPassword123!"), "Admin", role));
        String token = login("admin", "AdminPassword123!");
        mvc.perform(post("/api/v1/users").header("Authorization", "Bearer " + token).contentType("application/json")
                        .content("{\"username\":\"root2\",\"password\":\"RootPassword123!\",\"fullName\":\"Root 2\",\"role\":\"SUPER_ADMIN\",\"active\":true}"))
                .andExpect(status().isForbidden());
    }


    @Test void adminCanReadRoleCatalog() throws Exception {
        Role role = roles.findByCode(RoleCode.ADMIN).orElseThrow();
        users.save(new User("admin", encoder.encode("AdminPassword123!"), "Admin", role));
        String token = login("admin", "AdminPassword123!");
        mvc.perform(get("/api/v1/roles").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].code").exists());
    }

    @Test void rolesCatalogRequiresRoleRead() throws Exception {
        Role role = roles.findByCode(RoleCode.VIEWER).orElseThrow();
        users.save(new User("viewer", encoder.encode("ViewerPassword123!"), "Viewer", role));
        String token = login("viewer", "ViewerPassword123!");
        mvc.perform(get("/api/v1/roles").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
    }

    private String login(String username, String password) throws Exception {
        String body = mvc.perform(post("/api/v1/auth/login").contentType("application/json").content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode json = mapper.readTree(body); return json.get("accessToken").asText();
    }
}
