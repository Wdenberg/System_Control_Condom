package com.estacionamento.api.domain.parking;

import com.estacionamento.api.domain.apartment.Apartment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "parking_spaces")
public class ParkingSpace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20, unique = true)
    private String number;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apartment_id")
    private Apartment apartment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ParkingSpaceType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ParkingSpaceStatus status = ParkingSpaceStatus.AVAILABLE;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ParkingSpace() {
    }

    public ParkingSpace(String number, Apartment apartment, ParkingSpaceType type) {
        this.number = normalizeNumber(number);
        this.apartment = apartment;
        this.type = type == null ? ParkingSpaceType.ASSIGNED : type;
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

    public Apartment getApartment() {
        return apartment;
    }

    public ParkingSpaceType getType() {
        return type;
    }

    public ParkingSpaceStatus getStatus() {
        return status;
    }

    public boolean isActive() {
        return active;
    }

    public void markOccupied() {
        if (!active || status == ParkingSpaceStatus.BLOCKED) {
            throw new IllegalStateException("Parking space is not occupiable");
        }
        status = ParkingSpaceStatus.OCCUPIED;
    }

    public void markAvailable() {
        if (active && status != ParkingSpaceStatus.BLOCKED) {
            status = ParkingSpaceStatus.AVAILABLE;
        }
    }

    public void update(String number, Apartment apartment, ParkingSpaceType type) {
        this.number = normalizeNumber(number);
        this.apartment = apartment;
        this.type = type == null ? ParkingSpaceType.ASSIGNED : type;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    private static String normalizeNumber(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Parking space number is required");
        }
        return value.trim().toUpperCase();
    }
}
