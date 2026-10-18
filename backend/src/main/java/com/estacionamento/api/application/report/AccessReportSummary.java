package com.estacionamento.api.application.report;

import java.time.OffsetDateTime;
import java.util.Map;

public record AccessReportSummary(
        OffsetDateTime from,
        OffsetDateTime to,
        long total,
        long entries,
        long exits,
        long authorized,
        long denied,
        long overridden,
        Map<String, Long> decisions
) {
}
