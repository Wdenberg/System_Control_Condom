package com.estacionamento.api.web.report;

import com.estacionamento.api.application.report.AccessReportFilter;
import com.estacionamento.api.application.report.AccessReportRow;
import com.estacionamento.api.application.report.AccessReportSummary;
import com.estacionamento.api.application.report.GateReportRow;
import com.estacionamento.api.application.report.OccupancyReportRow;
import com.estacionamento.api.application.report.ReportService;
import com.estacionamento.api.domain.gate.GateOperationStatus;
import com.estacionamento.api.domain.movement.AccessDecision;
import com.estacionamento.api.domain.movement.MovementDirection;
import com.estacionamento.api.domain.movement.MovementSource;
import com.estacionamento.api.domain.movement.MovementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
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
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@PreAuthorize("hasAuthority('REPORT_READ')")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/access")
    public Page<AccessReportRow> access(
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to,
            @RequestParam(required = false) String plate,
            @RequestParam(required = false) MovementDirection direction,
            @RequestParam(required = false) MovementStatus status,
            @RequestParam(required = false) AccessDecision decision,
            @RequestParam(required = false) MovementSource source,
            @RequestParam(required = false) Long cameraId,
            @RequestParam(required = false) Long apartmentId,
            @PageableDefault(size = 30) Pageable pageable) {
        return reportService.access(new AccessReportFilter(from, to, plate, direction, status, decision, source, cameraId, apartmentId), pageable);
    }

    @GetMapping("/access/summary")
    public AccessReportSummary accessSummary(
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to,
            @RequestParam(required = false) String plate,
            @RequestParam(required = false) MovementDirection direction,
            @RequestParam(required = false) MovementStatus status,
            @RequestParam(required = false) AccessDecision decision,
            @RequestParam(required = false) MovementSource source,
            @RequestParam(required = false) Long cameraId,
            @RequestParam(required = false) Long apartmentId) {
        return reportService.accessSummary(new AccessReportFilter(from, to, plate, direction, status, decision, source, cameraId, apartmentId));
    }

    @GetMapping(value = "/access.csv", produces = "text/csv")
    public ResponseEntity<byte[]> accessCsv(
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to,
            @RequestParam(required = false) String plate,
            @RequestParam(required = false) MovementDirection direction,
            @RequestParam(required = false) MovementStatus status,
            @RequestParam(required = false) AccessDecision decision,
            @RequestParam(required = false) MovementSource source,
            @RequestParam(required = false) Long cameraId,
            @RequestParam(required = false) Long apartmentId) {
        List<AccessReportRow> rows = reportService.accessForCsv(new AccessReportFilter(from, to, plate, direction, status, decision, source, cameraId, apartmentId));
        byte[] body = reportService.csv(rows).getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDisposition(ContentDisposition.attachment().filename("relatorio-acessos.csv", StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(body);
    }

    @GetMapping("/occupancy")
    public List<OccupancyReportRow> occupancy() {
        return reportService.occupancy();
    }

    @GetMapping("/gates")
    public Page<GateReportRow> gates(
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to,
            @RequestParam(required = false) GateOperationStatus status,
            @PageableDefault(size = 30) Pageable pageable) {
        return reportService.gates(from, to, status, pageable);
    }
}
