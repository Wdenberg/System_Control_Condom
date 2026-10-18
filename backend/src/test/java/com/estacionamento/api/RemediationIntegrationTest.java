package com.estacionamento.api;

import com.estacionamento.api.application.gate.*;
import com.estacionamento.api.domain.role.*;
import com.estacionamento.api.domain.user.*;
import com.estacionamento.api.infrastructure.gate.GateCommandResult;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import tools.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class RemediationIntegrationTest {
    private static final String CAMERA_TOKEN="entry-test-secret-at-least-32-characters";
    @Container static final PostgreSQLContainer POSTGRES=new PostgreSQLContainer("postgres:16-alpine").withDatabaseName("garage").withUsername("garage").withPassword("test-password");
    @Container static final GenericContainer<?> REDIS=new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",POSTGRES::getJdbcUrl); r.add("spring.datasource.username",POSTGRES::getUsername);r.add("spring.datasource.password",POSTGRES::getPassword);
        r.add("spring.data.redis.host",REDIS::getHost);r.add("spring.data.redis.port",()->REDIS.getMappedPort(6379));
        r.add("app.security.jwt.secret-base64",()->"VGhpc0lzQVN0cm9uZ0xvY2FsVGVzdFNLb3lUb1VzZUZvclRlc3RpbmdPbkx5IQ==");
        r.add("app.initial-admin.enabled",()->false);r.add("app.security.hardening.rate-limit-enabled",()->false);
        r.add("app.hikvision.webhook.tokens.CAM-ENTRADA",()->CAMERA_TOKEN);
        r.add("app.hikvision.webhook.tokens.CAM-SAIDA",()->"exit-test-secret-at-least-32-characters");
        r.add("app.hikvision.cooldown",()->"PT0.001S");r.add("app.gate.enabled",()->true);r.add("app.gate.driver",()->"SIMULATED");
        r.add("app.gate.max-attempts",()->3);
        r.add("app.gate.operation-poll-delay-ms",()->3600000);
    }
    @Autowired MockMvc mvc; @Autowired JdbcTemplate jdbc; @Autowired ObjectMapper mapper;
    @Autowired UserRepository users; @Autowired RoleRepository roles; @Autowired PasswordEncoder encoder;
    @Autowired GateDispatchTransactions dispatch;
    @Autowired com.estacionamento.api.config.properties.GateProperties gateProperties;
    @Autowired Clock clock;
    @Autowired io.micrometer.core.instrument.MeterRegistry metrics;
    @Autowired com.estacionamento.api.application.realtime.RealtimeBroadcaster broadcaster;
    private String token;
    @BeforeEach void clean() throws Exception {
        jdbc.execute("TRUNCATE registration_requests,gate_operation_attempts,gate_operations,audit_logs,movements,hikvision_pictures,hikvision_events,vehicle_residents,vehicles,residents,parking_spaces,apartments,app_users RESTART IDENTITY CASCADE");
        jdbc.update("UPDATE cameras SET ip_address='192.168.15.43' WHERE name='CAM-ENTRADA'");
        users.save(new User("admin",encoder.encode("AdminPassword123!"),"Admin",roles.findByCode(RoleCode.ADMIN).orElseThrow()));
        String body=mvc.perform(post("/api/v1/auth/login").contentType("application/json").content("{\"username\":\"admin\",\"password\":\"AdminPassword123!\"}")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        token=mapper.readTree(body).get("accessToken").asText();
    }
    private String registration(String plate) {
        return "{\"requestId\":\""+UUID.randomUUID()+"\",\"apartmentNumber\":\"101\",\"apartmentActive\":true,\"residents\":[{\"fullName\":\"Resident Test\",\"active\":true}],\"parkingNumber\":\"101\",\"parkingType\":\"ASSIGNED\",\"parkingActive\":true,\"plate\":\""+plate+"\",\"vehicleActive\":true}";
    }
    private String create(String payload) throws Exception {
        return mvc.perform(post("/api/v1/registrations").header("Authorization","Bearer "+token).contentType("application/json").content(payload)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }
    private Long recognize() throws Exception {
        create(registration("ABC1D23"));
        String xml=Files.readString(Path.of("src/test/resources/hikvision/anpr-entry.xml")).replace("SOO3H61","ABC1D23");
        String body=mvc.perform(post("/api/v1/integrations/hikvision/events").contentType("application/xml").header("X-Hikvision-Webhook-Token",CAMERA_TOKEN).content(xml)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).get("movementId").asLong();
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(RoleCode.class)
    void sensitiveEndpointsFollowCurrentRolePermissions(RoleCode roleCode) throws Exception {
        User user=users.save(new User("profile",encoder.encode("ProfilePassword123!"),"Profile",roles.findByCode(roleCode).orElseThrow()));
        String login=mvc.perform(post("/api/v1/auth/login").contentType("application/json").content("{\"username\":\"profile\",\"password\":\"ProfilePassword123!\"}")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String bearer=mapper.readTree(login).get("accessToken").asText();
        Map<String,Permission> endpoints=Map.of("/api/v1/users",Permission.USER_READ,"/api/v1/roles",Permission.ROLE_READ,"/api/v1/audit",Permission.AUDIT_READ,"/api/v1/reports/occupancy",Permission.REPORT_READ,"/api/v1/gates/status",Permission.GATE_READ);
        for(var endpoint:endpoints.entrySet()) mvc.perform(get(endpoint.getKey()).header("Authorization","Bearer "+bearer)).andExpect(status().is(RolePermissions.of(roleCode).contains(endpoint.getValue())?200:403));
        mvc.perform(post("/api/v1/gates/test/ENTRY").header("Authorization","Bearer "+bearer).contentType("application/json").content("{\"justification\":\"Simulated controller commissioning test\"}")).andExpect(status().is(RolePermissions.of(roleCode).contains(Permission.GATE_TEST)?200:403));
    }
    @Test void missingConfidenceCannotProduceAnAuthorizedGateCommand() throws Exception {
        create(registration("ABC1D23"));String xml=Files.readString(Path.of("src/test/resources/hikvision/anpr-entry.xml")).replace("SOO3H61","ABC1D23").replaceAll("<confidenceLevel>[^<]*</confidenceLevel>","");
        mvc.perform(post("/api/v1/integrations/hikvision/events").contentType("application/xml").header("X-Hikvision-Webhook-Token",CAMERA_TOKEN).content(xml)).andExpect(status().isOk()).andExpect(jsonPath("$.accessDecision").value("LOW_CONFIDENCE"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM gate_operations",Integer.class)).isZero();
    }
    @Test void overrideOfLowConfidenceRecognitionBindsOccupancyResources() throws Exception {
        create(registration("ABC1D23"));
        String xml=Files.readString(Path.of("src/test/resources/hikvision/anpr-entry.xml")).replace("SOO3H61","ABC1D23").replaceAll("<confidenceLevel>[^<]*</confidenceLevel>","");
        String body=mvc.perform(post("/api/v1/integrations/hikvision/events").contentType("application/xml").header("X-Hikvision-Webhook-Token",CAMERA_TOKEN).content(xml)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long movement=mapper.readTree(body).get("movementId").asLong();
        mvc.perform(post("/api/v1/movements/"+movement+"/override").header("Authorization","Bearer "+token).contentType("application/json").content("{\"justification\":\"Operator verified the plate visually\"}")).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT reservation_active FROM movements",Boolean.class)).isTrue();
        assertThat(jdbc.queryForObject("SELECT parking_space_id FROM movements",Long.class)).isNotNull();
        var claim = dispatch.claim().orElseThrow();
        dispatch.finish(claim, GateCommandResult.success("simulated"));

        // Ajustado para incluir os campos completos esperados pelo validador do DTO
        mvc.perform(post("/api/v1/movements/"+movement+"/passage-confirmation").header("Authorization","Bearer "+token).contentType("application/json")
                        .content("{\"passed\":true,\"justification\":\"Passage observed after visual plate check\",\"controllerSettled\":true}"))
                .andExpect(status().isOk());

        assertThat(jdbc.queryForObject("SELECT inside_garage FROM vehicles",Boolean.class)).isTrue();
    }
    @Test void unknownVehicleMustBeRegisteredBeforeAnOverrideCanQueueACommand() throws Exception {
        String xml=Files.readString(Path.of("src/test/resources/hikvision/anpr-entry.xml"));
        String body=mvc.perform(post("/api/v1/integrations/hikvision/events").contentType("application/xml").header("X-Hikvision-Webhook-Token",CAMERA_TOKEN).content(xml)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long movement=mapper.readTree(body).get("movementId").asLong();
        mvc.perform(post("/api/v1/movements/"+movement+"/override").header("Authorization","Bearer "+token).contentType("application/json").content("{\"justification\":\"Unknown vehicle requires registration first\"}")).andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM gate_operations",Integer.class)).isZero();
    }
    @Test void revokedAuthorizationClosesAnExistingSseConnection() throws Exception {
        mvc.perform(get("/api/v1/realtime/events").header("Authorization","Bearer "+token).accept("text/event-stream")).andExpect(request().asyncStarted());
        assertThat(broadcaster.connectedClients()).isEqualTo(1);
        User user=users.findByUsernameIgnoreCase("admin").orElseThrow();user.revokeSessions();users.saveAndFlush(user);
        broadcaster.heartbeat();assertThat(broadcaster.connectedClients()).isZero();
    }
    @Test void registrationRollsBackEveryStepOnInvalidVehicle() throws Exception {
        mvc.perform(post("/api/v1/registrations").header("Authorization","Bearer "+token).contentType("application/json").content(registration("bad"))).andExpect(status().isBadRequest());
        for(String table:List.of("apartments","residents","parking_spaces","vehicles","registration_requests")) assertThat(jdbc.queryForObject("SELECT count(*) FROM "+table,Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_logs WHERE outcome='FAILURE'",Integer.class)).isPositive();
    }
    @Test void registrationIsIdempotentAcrossConcurrentHttpRequests() throws Exception {
        String payload=registration("ABC1D23");var executor=Executors.newFixedThreadPool(2);
        try {var a=executor.submit(()->create(payload));var b=executor.submit(()->create(payload));assertThat(mapper.readTree(a.get(15,TimeUnit.SECONDS))).isEqualTo(mapper.readTree(b.get(15,TimeUnit.SECONDS)));}
        finally {executor.shutdownNow();}
        assertThat(jdbc.queryForObject("SELECT count(*) FROM vehicles",Integer.class)).isEqualTo(1);
    }
    @Test void inactiveCreationRespectsEveryRequestedFlag() throws Exception {
        String payload=registration("ABC1D23").replace("\"active\":true","\"active\":false").replace("\"apartmentActive\":true","\"apartmentActive\":false").replace("\"parkingActive\":true","\"parkingActive\":false").replace("\"vehicleActive\":true","\"vehicleActive\":false");
        create(payload);
        for(String table:List.of("apartments","residents","parking_spaces","vehicles")) assertThat(jdbc.queryForObject("SELECT active FROM "+table,Boolean.class)).isFalse();
    }
    @Test void passwordResetRevokesExistingJwtAtNextRequest() throws Exception {
        User user=users.findByUsernameIgnoreCase("admin").orElseThrow();user.changePassword(encoder.encode("DifferentPassword123!"));users.saveAndFlush(user);
        mvc.perform(get("/api/v1/users").header("Authorization","Bearer "+token)).andExpect(status().isUnauthorized());
    }
    @Test void roleChangeRevokesExistingJwtAtNextRequest() throws Exception {
        User user=users.findByUsernameIgnoreCase("admin").orElseThrow();user.changeRole(roles.findByCode(RoleCode.VIEWER).orElseThrow());users.saveAndFlush(user);
        mvc.perform(get("/api/v1/users").header("Authorization","Bearer "+token)).andExpect(status().isUnauthorized());
    }
    @Test void disabledAccountCannotReuseJwt() throws Exception {
        User user=users.findByUsernameIgnoreCase("admin").orElseThrow();user.setActive(false);users.saveAndFlush(user);
        mvc.perform(get("/api/v1/auth/me").header("Authorization","Bearer "+token)).andExpect(status().isUnauthorized());
    }
    @Test void cameraAddressChangeDoesNotMakeTheSameExternalEventNew() throws Exception {
        recognize();jdbc.update("UPDATE cameras SET ip_address='203.0.113.5' WHERE name='CAM-ENTRADA'");
        String xml=Files.readString(Path.of("src/test/resources/hikvision/anpr-entry.xml")).replace("SOO3H61","ABC1D23").replace("192.168.15.43","203.0.113.5");
        mvc.perform(post("/api/v1/integrations/hikvision/events").contentType("application/xml").header("X-Hikvision-Webhook-Token",CAMERA_TOKEN).content(xml)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DUPLICATE"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM movements",Integer.class)).isEqualTo(1);
    }
    @Test void concurrentWebhookProducesOnlyOneMovementAndCommand() throws Exception {
        create(registration("ABC1D23"));String xml=Files.readString(Path.of("src/test/resources/hikvision/anpr-entry.xml")).replace("SOO3H61","ABC1D23");var executor=Executors.newFixedThreadPool(2);
        Callable<String> call=()->mvc.perform(post("/api/v1/integrations/hikvision/events").header("X-Hikvision-Webhook-Token",CAMERA_TOKEN).contentType("application/xml").content(xml)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        try {var a=executor.submit(call);var b=executor.submit(call);assertThat(mapper.readTree(a.get(15,TimeUnit.SECONDS)).get("eventId")).isEqualTo(mapper.readTree(b.get(15,TimeUnit.SECONDS)).get("eventId"));}finally{executor.shutdownNow();}
        assertThat(jdbc.queryForObject("SELECT count(*) FROM movements",Integer.class)).isEqualTo(1);assertThat(jdbc.queryForObject("SELECT count(*) FROM gate_operations",Integer.class)).isEqualTo(1);
    }
    @Test void twoWorkersCannotClaimSameOperationAndUnknownOutcomeIsNotRetried() throws Exception {
        double unknownBefore=metrics.get("garage_gate_operations_unknown_total").counter().count();
        recognize();var executor=Executors.newFixedThreadPool(2);List<Optional<GateDispatchTransactions.Claim>> claims;
        try {var a=executor.submit(()->dispatch.claim());var b=executor.submit(()->dispatch.claim());claims=List.of(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS));}finally{executor.shutdownNow();}
        assertThat(claims.stream().filter(Optional::isPresent).count()).isEqualTo(1);
        var claimed=claims.stream().flatMap(Optional::stream).findFirst().orElseThrow();
        dispatch.finish(claimed,GateCommandResult.unknown("Simulated delivery timeout"));
        dispatch.finish(claimed,GateCommandResult.success("Late duplicate completion must be ignored"));
        assertThat(metrics.get("garage_gate_operations_unknown_total").counter().count()).isEqualTo(unknownBefore+1);
        assertThat(dispatch.claim()).isEmpty();assertThat(jdbc.queryForObject("SELECT status FROM gate_operations",String.class)).isEqualTo("UNKNOWN");
        assertThat(jdbc.queryForObject("SELECT inside_garage FROM vehicles",Boolean.class)).isFalse();
    }
    @Test void pendingPassagePreventsParkingMutationThroughTheApi() throws Exception {
        recognize();
        Long space=jdbc.queryForObject("SELECT id FROM parking_spaces",Long.class);
        Long apartment=jdbc.queryForObject("SELECT id FROM apartments",Long.class);
        mvc.perform(put("/api/v1/parking-spaces/"+space).header("Authorization","Bearer "+token).contentType("application/json")
                .content("{\"number\":\"102\",\"apartmentId\":"+apartment+",\"type\":\"ASSIGNED\",\"active\":false}"))
                .andExpect(status().isUnprocessableEntity());
        assertThat(jdbc.queryForObject("SELECT active FROM parking_spaces",Boolean.class)).isTrue();
        assertThat(jdbc.queryForObject("SELECT number FROM parking_spaces",String.class)).isEqualTo("101");
    }
    @Test void expiredWorkerBecomesUnknownWithoutReopening() throws Exception {
        recognize();dispatch.claim().orElseThrow();jdbc.update("UPDATE gate_operations SET lease_until=?",OffsetDateTime.now().minusMinutes(1));dispatch.recoverExpired();
        assertThat(dispatch.claim()).isEmpty();assertThat(jdbc.queryForObject("SELECT status FROM gate_operations",String.class)).isEqualTo("UNKNOWN");
    }
    @Test void legacyUnknownOutcomeBlocksNewCommandsEvenWithoutAHistoricalReservation() throws Exception {
        recognize();dispatch.finish(dispatch.claim().orElseThrow(),GateCommandResult.unknown("Legacy uncertainty"));
        jdbc.update("UPDATE movements SET reservation_active=false");
        mvc.perform(post("/api/v1/movements/manual").header("Authorization","Bearer "+token).contentType("application/json")
                .content("{\"plate\":\"ABC1D23\",\"direction\":\"ENTRY\",\"justification\":\"Must wait for previous controller reconciliation\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.decision").value("OPERATION_PENDING"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM gate_operations",Integer.class)).isEqualTo(1);
    }
    @Test void reconciledPassageCannotBeRetriedAdministratively() throws Exception {
        Long movement = recognize();
        Long operation = jdbc.queryForObject("SELECT id FROM gate_operations WHERE movement_id=?", Long.class, movement);
        for (int attempt = 1; attempt <= gateProperties.maxAttempts(); attempt++) {
            var claim = dispatch.claim().orElseThrow();
            assertThat(claim.id()).isEqualTo(operation);
            dispatch.finish(claim, GateCommandResult.failure("Definite rejection"));
            assertThat(jdbc.queryForObject("SELECT attempt_count FROM gate_operations WHERE id=?", Integer.class, operation))
                    .isEqualTo(attempt);
            if (attempt < gateProperties.maxAttempts()) {
                assertThat(jdbc.queryForObject("SELECT status FROM gate_operations WHERE id=?", String.class, operation))
                        .isEqualTo("PENDING");
                // An operator declaration cannot cancel a command that is still queued.
                mvc.perform(post("/api/v1/movements/" + movement + "/passage-confirmation")
                                .header("Authorization", "Bearer " + token).contentType("application/json")
                                .content("{\"passed\":false,\"justification\":\"Operator must wait for the queued command\",\"controllerSettled\":true}"))
                        .andExpect(status().isConflict());
                // Advance only the retry eligibility; use real claims and attempt history without sleeping.
                jdbc.update("UPDATE gate_operations SET next_attempt_at=? WHERE id=?", OffsetDateTime.now(clock).minusSeconds(1), operation);
            }
        }
        assertThat(jdbc.queryForObject("SELECT status FROM gate_operations WHERE id=?", String.class, operation))
                .isEqualTo("EXHAUSTED");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM gate_operation_attempts WHERE operation_id=?", Integer.class, operation))
                .isEqualTo(gateProperties.maxAttempts());
        mvc.perform(post("/api/v1/movements/"+movement+"/passage-confirmation")
                        .header("Authorization","Bearer "+token)
                        .contentType("application/json")
                        .content("{\"passed\":false,\"justification\":\"Operator verified no vehicle passed\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.passageState").value("FAILED"));

        mvc.perform(post("/api/v1/gates/operations/"+operation+"/retry").header("Authorization","Bearer "+token).contentType("application/json").content("{\"justification\":\"Repeat must be rejected after reconciliation\"}")).andExpect(status().isConflict());
        assertThat(dispatch.claim()).isEmpty();
        assertThat(jdbc.queryForObject("SELECT reservation_active FROM movements WHERE id=?", Boolean.class, movement)).isFalse();
        assertThat(jdbc.queryForObject("SELECT inside_garage FROM vehicles", Boolean.class)).isFalse();
        assertThat(jdbc.queryForObject("SELECT status FROM gate_operations WHERE id=?", String.class, operation)).isEqualTo("EXHAUSTED");
    }

    @Test void unknownOutcomeRequiresControllerVerificationBeforeReconciliation() throws Exception {
        Long movement=recognize();var claim=dispatch.claim().orElseThrow();dispatch.finish(claim,GateCommandResult.unknown("Uncertain delivery"));

        mvc.perform(post("/api/v1/movements/"+movement+"/passage-confirmation")
                        .header("Authorization","Bearer "+token).contentType("application/json")
                        .content("{\"passed\":false,\"justification\":\"No controller verification provided\"}"))
                .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT passage_state FROM movements WHERE id=?", String.class, movement)).isEqualTo("UNKNOWN");
        assertThat(jdbc.queryForObject("SELECT reservation_active FROM movements WHERE id=?", Boolean.class, movement)).isTrue();
        assertThat(dispatch.claim()).isEmpty();

        mvc.perform(post("/api/v1/movements/"+movement+"/passage-confirmation")
                        .header("Authorization","Bearer "+token)
                        .contentType("application/json")
                        .content("{\"passed\":false,\"justification\":\"Verified via controller check\",\"controllerSettled\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.passageState").value("FAILED"));
        assertThat(jdbc.queryForObject("SELECT reservation_active FROM movements WHERE id=?", Boolean.class, movement)).isFalse();
        assertThat(jdbc.queryForObject("SELECT inside_garage FROM vehicles", Boolean.class)).isFalse();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM gate_operation_attempts WHERE operation_id=?", Integer.class, claim.id())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_logs WHERE action='PASSAGE_RECONCILIATION' AND details LIKE '%controllerSettled=true%'", Integer.class)).isEqualTo(1);
    }

    @Test void acceptedCommandDoesNotConfirmOccupancyUntilObservedPassage() throws Exception {
        Long movement=recognize();var claim=dispatch.claim().orElseThrow();dispatch.finish(claim,GateCommandResult.success("simulated"));
        assertThat(jdbc.queryForObject("SELECT inside_garage FROM vehicles",Boolean.class)).isFalse();

        mvc.perform(post("/api/v1/movements/"+movement+"/passage-confirmation")
                        .header("Authorization","Bearer "+token)
                        .contentType("application/json")
                        .content("{\"passed\":true,\"justification\":\"Passage observed by authorized operator\",\"controllerSettled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passageState").value("COMPLETED"));

        assertThat(jdbc.queryForObject("SELECT inside_garage FROM vehicles",Boolean.class)).isTrue();

        // Removida a referência à coluna inexistente 'occupied', validando apenas pelo status correto
        assertThat(jdbc.queryForObject("SELECT count(*) FROM parking_spaces WHERE status = 'OCCUPIED'", Integer.class)).isPositive();
    }
}
