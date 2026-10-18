package com.estacionamento.api.domain.movement;

import com.estacionamento.api.domain.camera.Camera;
import com.estacionamento.api.domain.hikvision.HikvisionEvent;
import com.estacionamento.api.domain.parking.ParkingSpace;
import com.estacionamento.api.domain.user.User;
import com.estacionamento.api.domain.vehicle.Vehicle;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(
        name = "movements",
        uniqueConstraints = @UniqueConstraint(name = "uk_movement_hikvision_event", columnNames = "hikvision_event_id")
)
public class Movement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "camera_id")
    private Camera camera;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parking_space_id")
    private ParkingSpace parkingSpace;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hikvision_event_id")
    private HikvisionEvent hikvisionEvent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_user_id")
    private User operator;

    @Column(nullable = false, length = 20)
    private String plate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MovementDirection direction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MovementSource source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MovementStatus status;
    @Enumerated(EnumType.STRING)
    @Column(name = "passage_state", nullable = false, length = 32)
    private PassageState passageState;
    @Column(name = "reservation_active", nullable = false)
    private boolean reservationActive;
    @Column(name = "passage_confirmed_at")
    private OffsetDateTime passageConfirmedAt;
    public PassageState getPassageState() { return passageState; }
    public boolean isReservationActive() { return reservationActive; }
    public void passageState(PassageState state) { this.passageState = state; }
    public void reservePassage() { this.reservationActive = vehicle != null; }
    public void reconcile(boolean passed, OffsetDateTime now) {
        if (passageState == PassageState.COMPLETED || passageState == PassageState.FAILED) throw new IllegalStateException("Passage is already reconciled");
        if (status == MovementStatus.DENIED) throw new IllegalStateException("Denied movement cannot be confirmed");
        passageState = passed ? PassageState.COMPLETED : PassageState.FAILED;
        passageConfirmedAt = passed ? now : null;
        reservationActive = false;
    }


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AccessDecision accessDecision;

    @Column(name = "confidence_level")
    private Integer confidenceLevel;

    @Column(name = "event_time", nullable = false)
    private OffsetDateTime eventTime;

    @Column(length = 1000)
    private String justification;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected Movement() {
    }

    public Movement(Vehicle vehicle, Camera camera, ParkingSpace parkingSpace, HikvisionEvent hikvisionEvent,
                    User operator, String plate, MovementDirection direction, MovementSource source,
                    MovementStatus status, AccessDecision accessDecision, Integer confidenceLevel,
                    OffsetDateTime eventTime, String justification) {
        this.vehicle = vehicle;
        this.camera = camera;
        this.parkingSpace = parkingSpace;
        this.hikvisionEvent = hikvisionEvent;
        this.operator = operator;
        this.plate = plate;
        this.direction = direction;
        this.source = source;
        this.status = status;
        this.passageState = status == MovementStatus.DENIED ? PassageState.DENIED : PassageState.AUTHORIZED;
        this.accessDecision = accessDecision;
        this.confidenceLevel = confidenceLevel;
        this.eventTime = eventTime;
        this.justification = justification;
    }

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public Long getId() {
        return id;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public Camera getCamera() {
        return camera;
    }

    public ParkingSpace getParkingSpace() {
        return parkingSpace;
    }

    public HikvisionEvent getHikvisionEvent() {
        return hikvisionEvent;
    }

    public User getOperator() {
        return operator;
    }

    public String getPlate() {
        return plate;
    }

    public MovementDirection getDirection() {
        return direction;
    }

    public MovementSource getSource() {
        return source;
    }

    public MovementStatus getStatus() {
        return status;
    }

    public AccessDecision getAccessDecision() {
        return accessDecision;
    }

    public Integer getConfidenceLevel() {
        return confidenceLevel;
    }

    public OffsetDateTime getEventTime() {
        return eventTime;
    }

    public String getJustification() {
        return justification;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void override(User operator, String justification, Vehicle vehicle, ParkingSpace parkingSpace) {
        if (status != MovementStatus.DENIED || vehicle == null || !plate.equals(vehicle.getPlate())) {
            throw new IllegalStateException("Override requires a denied recognition and its registered vehicle");
        }
        this.vehicle = vehicle;
        this.parkingSpace = parkingSpace;
        this.operator = operator;
        this.source = MovementSource.MANUAL_OVERRIDE;
        this.status = MovementStatus.OVERRIDDEN;
        this.passageState = PassageState.AUTHORIZED;
        this.accessDecision = AccessDecision.OVERRIDE_AUTHORIZED;
        this.justification = justification;
    }
}
