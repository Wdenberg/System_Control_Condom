package com.estacionamento.api.domain.apartment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "apartments")
public class Apartment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20, unique = true)
    private String number;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Apartment() {
    }

    public Apartment(String number) {
        this.number = normalizeNumber(number);
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getNumber() {
        return number;
    }

    public boolean isActive() {
        return active;
    }

    public void rename(String number) {
        this.number = normalizeNumber(number);
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private static String normalizeNumber(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Apartment number is required");
        }
        String normalized = value.trim().toUpperCase();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Apartment number is required");
        }
        return normalized;
    }
}
