package com.estacionamento.api.application.audit;

import com.estacionamento.api.domain.audit.AuditLog;
import com.estacionamento.api.domain.audit.AuditLogRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditServiceTest {

    private final AuditLogRepository repository = Mockito.mock(AuditLogRepository.class);
    private final AuditService service = new AuditService(repository);

    @AfterEach
    void clearContext() {
        AuditRequestContext.clear();
    }

    @Test
    void recordShouldCaptureRequestContext() {
        AuditRequestContext.set(new AuditRequestContext.Context(
                "corr-123", "POST", "/api/v1/movements/10/override", "10.0.0.10", "JUnit"));
        when(repository.save(Mockito.any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuditLog result = service.record(null, "MOVEMENT_OVERRIDE", "MOVEMENT", "10", "justification", "details");

        assertThat(result.getCorrelationId()).isEqualTo("corr-123");
        assertThat(result.getRequestMethod()).isEqualTo("POST");
        assertThat(result.getRequestPath()).isEqualTo("/api/v1/movements/10/override");
        assertThat(result.getIpAddress()).isEqualTo("10.0.0.10");
        assertThat(result.getUserAgent()).isEqualTo("JUnit");
        assertThat(result.getOutcome()).isEqualTo("SUCCESS");
    }

    @Test
    void listShouldDelegateWithFiltersAndPageable() {
        AuditFilter filter = new AuditFilter(null, null, "admin", "GATE_TEST", "GATE", "1", "127.0.0.1", "corr-1");
        PageRequest pageable = PageRequest.of(0, 30, Sort.by("createdAt").descending());
        when(repository.findAll(Mockito.any(org.springframework.data.jpa.domain.Specification.class), Mockito.eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        service.list(filter, pageable);

        verify(repository).findAll(Mockito.any(org.springframework.data.jpa.domain.Specification.class), Mockito.eq(pageable));
    }
}
