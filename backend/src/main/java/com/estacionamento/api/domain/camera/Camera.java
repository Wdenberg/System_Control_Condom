package com.estacionamento.api.domain.camera;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "cameras")
public class Camera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, unique = true)
    private String name;

    @Column(name = "ip_address", nullable = false, length = 45, unique = true)
    private String ipAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CameraDirection direction;

    @Column(nullable = false, length = 32)
    private String vendor;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(name = "channel_id", nullable = false)
    private Integer channelId;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Camera() {
    }

    public Camera(String name, String ipAddress, CameraDirection direction, String vendor, String model,
                   Integer channelId, boolean enabled) {
        this.name = name;
        this.ipAddress = ipAddress;
        this.direction = direction;
        this.vendor = vendor;
        this.model = model;
        this.channelId = channelId;
        this.enabled = enabled;
    }

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public CameraDirection getDirection() {
        return direction;
    }

    public String getVendor() {
        return vendor;
    }

    public String getModel() {
        return model;
    }

    public Integer getChannelId() {
        return channelId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
