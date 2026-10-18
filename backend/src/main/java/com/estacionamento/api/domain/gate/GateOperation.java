package com.estacionamento.api.domain.gate;

import com.estacionamento.api.domain.movement.Movement;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "gate_operations", uniqueConstraints = @UniqueConstraint(name = "uk_gate_operation_movement", columnNames = "movement_id"))
public class GateOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operation_key", nullable = false, length = 64, unique = true)
    private String operationKey;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movement_id", nullable = false, unique = true)
    private Movement movement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private GateCommand command;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private GateOperationStatus status;

    @Column(nullable = false, length = 32)
    private String driver;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;
    @Column(name = "claim_token", length = 64)
    private String claimToken;
    @jakarta.persistence.Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;
    @Column(name = "lease_until")
    private OffsetDateTime leaseUntil;
    public OffsetDateTime getLeaseUntil() { return leaseUntil; }
    public String getClaimToken() { return claimToken; }
    public void claim(String token, OffsetDateTime until, OffsetDateTime now) {
        if (status != GateOperationStatus.PENDING) throw new IllegalStateException("Operation is not pending");
        status = GateOperationStatus.PROCESSING; claimToken = token; leaseUntil = until; registerAttempt(now);
    }
    public void markUnknown(String reason) { status = GateOperationStatus.UNKNOWN; lastError = reason; nextAttemptAt = null; leaseUntil = null; }


    @Column(name = "last_error", length = 2000)
    private String lastError;

    @Column(name = "requested_at", nullable = false)
    private OffsetDateTime requestedAt;

    @Column(name = "next_attempt_at")
    private OffsetDateTime nextAttemptAt;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected GateOperation() {
    }

    private GateOperation(Movement movement, GateOperationStatus status, String driver, OffsetDateTime now) {
        this.operationKey = UUID.randomUUID().toString();
        this.movement = movement;
        this.command = GateCommand.OPEN;
        this.status = status;
        this.driver = driver;
        this.attemptCount = 0;
        this.requestedAt = now;
        this.nextAttemptAt = status == GateOperationStatus.PENDING ? now : null;
    }

    public static GateOperation pending(Movement movement, String driver, OffsetDateTime now) {
        return new GateOperation(movement, GateOperationStatus.PENDING, driver, now);
    }

    public static GateOperation disabled(Movement movement, String driver, OffsetDateTime now) {
        return new GateOperation(movement, GateOperationStatus.DISABLED, driver, now);
    }

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void registerAttempt(OffsetDateTime now) {
        attemptCount++;
        lastError = null;
    }

    public void markSent(OffsetDateTime now) {
        status = GateOperationStatus.SENT;
        sentAt = now;
        nextAttemptAt = null;
        lastError = null;
    }

    public void markFailure(String error, OffsetDateTime nextAttemptAt, boolean exhausted) {
        lastError = error;
        this.nextAttemptAt = nextAttemptAt;
        if (exhausted) {
            status = GateOperationStatus.EXHAUSTED;
            this.nextAttemptAt = null;
        } else {
            status = GateOperationStatus.PENDING;
        }
    }

    public Long getId() { return id; }
    public String getOperationKey() { return operationKey; }
    public Movement getMovement() { return movement; }
    public GateCommand getCommand() { return command; }
    public GateOperationStatus getStatus() { return status; }
    public String getDriver() { return driver; }
    public int getAttemptCount() { return attemptCount; }
    public String getLastError() { return lastError; }
    public OffsetDateTime getRequestedAt() { return requestedAt; }
    public OffsetDateTime getNextAttemptAt() { return nextAttemptAt; }
    public OffsetDateTime getSentAt() { return sentAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
