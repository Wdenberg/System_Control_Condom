package com.estacionamento.api.domain.user;

import com.estacionamento.api.domain.role.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "app_users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 160)
    private String fullName;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "session_version", nullable = false)
    private long sessionVersion;

    @jakarta.persistence.Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    public long getSessionVersion() { return sessionVersion; }

    public void revokeSessions() { sessionVersion++; }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected User() {
    }

    public User(String username, String passwordHash, String fullName, Role role) {
        this.username = normalizeUsername(username);
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash must not be null");
        this.fullName = Objects.requireNonNull(fullName, "fullName must not be null").trim();
        this.role = Objects.requireNonNull(role, "role must not be null");
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public Role getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        if (this.active != active) revokeSessions();
        this.active = active;
    }

    public void changeFullName(String fullName) {
        String normalized = Objects.requireNonNull(fullName, "fullName must not be null").trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("fullName must not be blank");
        }
        this.fullName = normalized;
    }

    public void changePassword(String passwordHash) {
        revokeSessions();
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash must not be null");
    }

    public void changeRole(Role role) {
        if (this.role.getCode() != role.getCode()) revokeSessions();
        this.role = Objects.requireNonNull(role, "role must not be null");
    }

    private static String normalizeUsername(String username) {
        String normalized = Objects.requireNonNull(username, "username must not be null").trim().toLowerCase(java.util.Locale.ROOT);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        return normalized;
    }
}
