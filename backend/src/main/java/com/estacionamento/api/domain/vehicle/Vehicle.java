package com.estacionamento.api.domain.vehicle;

import com.estacionamento.api.domain.parking.ParkingSpace;
import com.estacionamento.api.domain.resident.Resident;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 7, unique = true)
    private String plate;

    @Column(length = 80)
    private String brand;

    @Column(length = 80)
    private String model;

    @Column(length = 40)
    private String color;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parking_space_id")
    private ParkingSpace parkingSpace;

    @ManyToMany
    @JoinTable(
            name = "vehicle_residents",
            joinColumns = @JoinColumn(name = "vehicle_id"),
            inverseJoinColumns = @JoinColumn(name = "resident_id"),
            uniqueConstraints = @UniqueConstraint(name = "uk_vehicle_resident", columnNames = {"vehicle_id", "resident_id"})
    )
    private Set<Resident> residents = new HashSet<>();

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "inside_garage", nullable = false)
    private boolean insideGarage = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Vehicle() {
    }

    public Vehicle(String plate, String brand, String model, String color, ParkingSpace parkingSpace, Set<Resident> residents) {
        this.plate = normalizePlate(plate);
        this.brand = cleanOptional(brand);
        this.model = cleanOptional(model);
        this.color = cleanOptional(color);
        this.parkingSpace = parkingSpace;
        this.residents = new HashSet<>(residents);
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

    public String getPlate() {
        return plate;
    }

    public String getBrand() {
        return brand;
    }

    public String getModel() {
        return model;
    }

    public String getColor() {
        return color;
    }

    public ParkingSpace getParkingSpace() {
        return parkingSpace;
    }

    public Set<Resident> getResidents() {
        return Set.copyOf(residents);
    }

    public boolean isActive() {
        return active;
    }

    public boolean isInsideGarage() {
        return insideGarage;
    }

    public void markInsideGarage() {
        this.insideGarage = true;
    }

    public void markOutsideGarage() {
        this.insideGarage = false;
    }

    public void update(String plate, String brand, String model, String color, ParkingSpace parkingSpace, Set<Resident> residents) {
        this.plate = normalizePlate(plate);
        this.brand = cleanOptional(brand);
        this.model = cleanOptional(model);
        this.color = cleanOptional(color);
        this.parkingSpace = parkingSpace;
        this.residents.clear();
        this.residents.addAll(residents);
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    private static String normalizePlate(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Vehicle plate is required");
        }
        String normalized = value.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (!normalized.matches("[A-Z0-9]{7}")) {
            throw new IllegalArgumentException("Vehicle plate must contain exactly 7 alphanumeric characters");
        }
        return normalized;
    }

    private static String cleanOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
