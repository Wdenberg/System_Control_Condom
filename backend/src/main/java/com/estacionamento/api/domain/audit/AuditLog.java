package com.estacionamento.api.domain.audit;

import com.estacionamento.api.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 80)
    private String entityType;

    @Column(name = "entity_id", length = 80)
    private String entityId;

    @Column(length = 1000)
    private String justification;

    @Column(length = 2000)
    private String details;

    @Column(length = 16, nullable = false)
    private String outcome = "SUCCESS";

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "request_method", length = 16)
    private String requestMethod;

    @Column(name = "request_path", length = 512)
    private String requestPath;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected AuditLog() {
    }

    public AuditLog(User user, String action, String entityType, String entityId, String justification, String details) {
        this.user = user;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.justification = justification;
        this.details = details;
    }

    public void enrichRequestContext(String correlationId, String requestMethod, String requestPath,
                                     String ipAddress, String userAgent) {
        this.correlationId = correlationId;
        this.requestMethod = requestMethod;
        this.requestPath = requestPath;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        if (outcome == null || outcome.isBlank()) {
            outcome = "SUCCESS";
        }
    }

    public void outcome(boolean success) { outcome = success ? "SUCCESS" : "FAILURE"; }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getAction() { return action; }
    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
    public String getJustification() { return justification; }
    public String getDetails() { return details; }
    public String getOutcome() { return outcome; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }
    public String getCorrelationId() { return correlationId; }
    public String getRequestMethod() { return requestMethod; }
    public String getRequestPath() { return requestPath; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
