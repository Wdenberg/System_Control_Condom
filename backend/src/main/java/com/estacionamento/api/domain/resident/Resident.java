package com.estacionamento.api.domain.resident;

import com.estacionamento.api.domain.apartment.Apartment;
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

@Entity
@Table(name = "residents")
public class Resident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String fullName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "apartment_id", nullable = false)
    private Apartment apartment;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Resident() {
    }

    public Resident(String fullName, Apartment apartment) {
        this.fullName = normalizeName(fullName);
        this.apartment = apartment;
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

    public String getFullName() {
        return fullName;
    }

    public Apartment getApartment() {
        return apartment;
    }

    public boolean isActive() {
        return active;
    }

    public void rename(String fullName) {
        this.fullName = normalizeName(fullName);
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void moveTo(Apartment apartment) {
        if (apartment == null) {
            throw new IllegalArgumentException("Apartment is required");
        }
        this.apartment = apartment;
    }

    private static String normalizeName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Resident full name is required");
        }
        return value.trim();
    }
}
