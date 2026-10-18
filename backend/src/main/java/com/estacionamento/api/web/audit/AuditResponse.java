package com.estacionamento.api.web.audit;

import com.estacionamento.api.domain.audit.AuditLog;

import java.time.OffsetDateTime;

public record AuditResponse(
        Long id,
        Long userId,
        String username,
        String action,
        String entityType,
        String entityId,
        String justification,
        String details,
        String outcome,
        String ipAddress,
        String userAgent,
        String correlationId,
        String requestMethod,
        String requestPath,
        OffsetDateTime createdAt
) {
    public static AuditResponse from(AuditLog log) {
        return new AuditResponse(
                log.getId(),
                log.getUser() == null ? null : log.getUser().getId(),
                log.getUser() == null ? null : log.getUser().getUsername(),
                log.getAction(), log.getEntityType(), log.getEntityId(),
                log.getJustification(), log.getDetails(), log.getOutcome(),
                log.getIpAddress(), log.getUserAgent(), log.getCorrelationId(),
                log.getRequestMethod(), log.getRequestPath(), log.getCreatedAt()
        );
    }
}
