package com.estacionamento.api.application.report;

import com.estacionamento.api.config.properties.DashboardProperties;
import com.estacionamento.api.domain.gate.GateOperationRepository;
import com.estacionamento.api.domain.movement.MovementRepository;
import com.estacionamento.api.domain.parking.ParkingSpaceRepository;
import com.estacionamento.api.domain.vehicle.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock MovementRepository movementRepository;
    @Mock ParkingSpaceRepository parkingSpaceRepository;
    @Mock VehicleRepository vehicleRepository;
    @Mock GateOperationRepository gateOperationRepository;

    @InjectMocks ReportService reportService;

    @Test
    void shouldEscapeCsvFieldsAndIncludeUtf8Bom() {
        AccessReportRow row = new AccessReportRow(
                10L,
                ZonedDateTime.parse("2026-09-13T10:00:00-03:00").toOffsetDateTime(),
                "ABC1D23",
                "ENTRY",
                "HIKVISION",
                "AUTHORIZED",
                "AUTHORIZED",
                91,
                "João; Silva",
                "101",
                "127",
                "CAM-ENTRADA",
                "192.168.15.43"
        );

        String csv = reportService.csv(List.of(row));

        assertThat(csv).startsWith("\uFEFFID;Data/Hora;");
        assertThat(csv).contains("\"João; Silva\"");
        assertThat(csv).contains("\"ABC1D23\"");
    }

    @Test
    void shouldRejectInvalidPeriod() {
        DashboardProperties properties = new DashboardProperties("America/Recife", java.time.Duration.ofSeconds(5));
        ReportService service = new ReportService(
                movementRepository,
                parkingSpaceRepository,
                vehicleRepository,
                gateOperationRepository,
                properties,
                Clock.system(ZoneId.of("America/Recife"))
        );

        var from = ZonedDateTime.parse("2026-09-13T12:00:00-03:00").toOffsetDateTime();
        var to = from.minusMinutes(1);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.accessSummary(
                        new AccessReportFilter(from, to, null, null, null, null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Report start must be before report end");
    }
}
