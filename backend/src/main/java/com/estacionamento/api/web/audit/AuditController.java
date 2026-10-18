package com.estacionamento.api.web.audit;

import com.estacionamento.api.application.audit.AuditFilter;
import com.estacionamento.api.application.audit.AuditService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('AUDIT_READ')")
    public Page<AuditResponse> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) String ipAddress,
            @RequestParam(required = false) String correlationId,
            @PageableDefault(size = 30, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        if (pageable.getPageSize() > 100) {
            pageable = PageRequest.of(pageable.getPageNumber(), 100, pageable.getSort());
        }
        AuditFilter filter = new AuditFilter(from, to, username, action, entityType, entityId, ipAddress, correlationId);
        return auditService.list(filter, pageable).map(AuditResponse::from);
    }

    @GetMapping(value = "/csv", produces = "text/csv")
    @PreAuthorize("hasAuthority('AUDIT_READ')")
    public ResponseEntity<byte[]> csv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) String ipAddress,
            @RequestParam(required = false) String correlationId
    ) {
        AuditFilter filter = new AuditFilter(from, to, username, action, entityType, entityId, ipAddress, correlationId);
        var logs = auditService.list(filter, PageRequest.of(0, 50_000, Sort.by("createdAt").descending())).getContent();

        StringBuilder csv = new StringBuilder("ID;Data;Usuário;Ação;Entidade;ID Entidade;Resultado;IP;Método;Caminho;Correlation ID;Justificativa;Detalhes\n");
        for (AuditResponse row : logs.stream().map(AuditResponse::from).toList()) {
            csv.append(cell(row.id())).append(';')
                    .append(cell(row.createdAt())).append(';')
                    .append(cell(row.username())).append(';')
                    .append(cell(row.action())).append(';')
                    .append(cell(row.entityType())).append(';')
                    .append(cell(row.entityId())).append(';')
                    .append(cell(row.outcome())).append(';')
                    .append(cell(row.ipAddress())).append(';')
                    .append(cell(row.requestMethod())).append(';')
                    .append(cell(row.requestPath())).append(';')
                    .append(cell(row.correlationId())).append(';')
                    .append(cell(row.justification())).append(';')
                    .append(cell(row.details())).append('\n');
        }

        byte[] body = ("\uFEFF" + csv).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=auditoria.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(body);
    }

    private static String cell(Object value) {
        if (value == null) return "";
        String text = String.valueOf(value).replace("\"", "\"\"");
        return "\"" + text + "\"";
    }
}
