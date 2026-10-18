package com.estacionamento.api.domain.hikvision;

import com.estacionamento.api.domain.camera.Camera;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(
        name = "hikvision_events",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_hikvision_event_source_external",
                columnNames = {"source_ip_address", "external_event_id"}
        )
)
public class HikvisionEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "camera_id")
    private Camera camera;

    @Column(name = "source_ip_address", nullable = false, length = 45)
    private String sourceIpAddress;

    @Column(name = "remote_ip_address", length = 45)
    private String remoteIpAddress;

    @Column(name = "external_event_id", nullable = false, length = 128)
    private String externalEventId;

    @Column(name = "hikvision_uuid", length = 128)
    private String hikvisionUuid;

    @Column(name = "event_type", nullable = false, length = 32)
    private String eventType;

    @Column(name = "event_state", length = 32)
    private String eventState;

    @Column(name = "event_description", length = 128)
    private String eventDescription;

    @Column(name = "channel_id")
    private Integer channelId;

    @Column(name = "channel_name", length = 128)
    private String channelName;

    @Column(name = "mac_address", length = 32)
    private String macAddress;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @Column(name = "received_at", nullable = false)
    private OffsetDateTime receivedAt;

    @Column(name = "plate", length = 20)
    private String plate;

    @Column(name = "original_plate", length = 32)
    private String originalPlate;

    @Column(name = "country_code", length = 16)
    private String countryCode;

    @Column(name = "line_number")
    private Integer lineNumber;

    @Column(name = "capture_direction", length = 16)
    private String captureDirection;

    @Column(name = "confidence_level")
    private Integer confidenceLevel;

    @Column(name = "plate_type", length = 32)
    private String plateType;

    @Column(name = "plate_color", length = 32)
    private String plateColor;

    @Column(name = "license_bright")
    private Integer licenseBright;

    @Column(name = "dangmark", length = 16)
    private String dangmark;

    @Column(name = "two_wheel_vehicle", length = 16)
    private String twoWheelVehicle;

    @Column(name = "three_wheel_vehicle", length = 16)
    private String threeWheelVehicle;

    @Column(name = "plate_char_believe", length = 512)
    private String plateCharBelieve;

    @Column(name = "vehicle_type", length = 64)
    private String vehicleType;

    @Column(name = "detect_dir")
    private Integer detectDir;

    @Column(name = "detect_type")
    private Integer detectType;

    @Column(name = "alarm_data_type")
    private Integer alarmDataType;

    @Column(name = "vehicle_index")
    private Integer vehicleIndex;

    @Column(name = "vehicle_color_depth")
    private Integer vehicleColorDepth;

    @Column(name = "vehicle_color", length = 64)
    private String vehicleColor;

    @Column(name = "vehicle_length")
    private Integer vehicleLength;

    @Column(name = "vehicle_logo_recog")
    private Integer vehicleLogoRecog;

    @Column(name = "vehicle_sub_logo_recog")
    private Integer vehicleSubLogoRecog;

    @Column(name = "vehicle_model")
    private Integer vehicleModel;

    @Column(name = "vehicle_list_name", length = 64)
    private String vehicleListName;

    @Column(name = "plate_size")
    private Integer plateSize;

    @Column(name = "picture_count")
    private Integer pictureCount;

    @Column(name = "is_data_retransmission", nullable = false)
    private boolean dataRetransmission;

    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 24)
    private HikvisionProcessingStatus processingStatus;

    @Column(name = "processing_error", length = 2000)
    private String processingError;

    @Column(name = "raw_xml", nullable = false, columnDefinition = "TEXT")
    private String rawXml;

    protected HikvisionEvent() {
    }

    public HikvisionEvent(String sourceIpAddress, String remoteIpAddress, String externalEventId,
                          String hikvisionUuid, String eventType, String eventState, String eventDescription,
                          Integer channelId, String channelName, String macAddress, OffsetDateTime occurredAt,
                          String plate, String originalPlate, String countryCode, Integer lineNumber,
                          String captureDirection, Integer confidenceLevel, String plateType, String plateColor,
                          Integer licenseBright, String dangmark, String twoWheelVehicle, String threeWheelVehicle,
                          String plateCharBelieve, String vehicleType, Integer detectDir, Integer detectType,
                          Integer alarmDataType, Integer vehicleIndex, Integer vehicleColorDepth, String vehicleColor,
                          Integer vehicleLength, Integer vehicleLogoRecog, Integer vehicleSubLogoRecog,
                          Integer vehicleModel, String vehicleListName, Integer plateSize, Integer pictureCount,
                          boolean dataRetransmission, String payloadHash, HikvisionProcessingStatus processingStatus,
                          String processingError, String rawXml, Camera camera) {
        this.sourceIpAddress = sourceIpAddress;
        this.remoteIpAddress = remoteIpAddress;
        this.externalEventId = externalEventId;
        this.hikvisionUuid = hikvisionUuid;
        this.eventType = eventType;
        this.eventState = eventState;
        this.eventDescription = eventDescription;
        this.channelId = channelId;
        this.channelName = channelName;
        this.macAddress = macAddress;
        this.occurredAt = occurredAt;
        this.plate = plate;
        this.originalPlate = originalPlate;
        this.countryCode = countryCode;
        this.lineNumber = lineNumber;
        this.captureDirection = captureDirection;
        this.confidenceLevel = confidenceLevel;
        this.plateType = plateType;
        this.plateColor = plateColor;
        this.licenseBright = licenseBright;
        this.dangmark = dangmark;
        this.twoWheelVehicle = twoWheelVehicle;
        this.threeWheelVehicle = threeWheelVehicle;
        this.plateCharBelieve = plateCharBelieve;
        this.vehicleType = vehicleType;
        this.detectDir = detectDir;
        this.detectType = detectType;
        this.alarmDataType = alarmDataType;
        this.vehicleIndex = vehicleIndex;
        this.vehicleColorDepth = vehicleColorDepth;
        this.vehicleColor = vehicleColor;
        this.vehicleLength = vehicleLength;
        this.vehicleLogoRecog = vehicleLogoRecog;
        this.vehicleSubLogoRecog = vehicleSubLogoRecog;
        this.vehicleModel = vehicleModel;
        this.vehicleListName = vehicleListName;
        this.plateSize = plateSize;
        this.pictureCount = pictureCount;
        this.dataRetransmission = dataRetransmission;
        this.payloadHash = payloadHash;
        this.processingStatus = processingStatus;
        this.processingError = processingError;
        this.rawXml = rawXml;
        this.camera = camera;
    }

    @PrePersist
    void onCreate() {
        receivedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public Long getId() {
        return id;
    }

    public Camera getCamera() {
        return camera;
    }

    public String getSourceIpAddress() {
        return sourceIpAddress;
    }

    public String getRemoteIpAddress() {
        return remoteIpAddress;
    }

    public String getExternalEventId() {
        return externalEventId;
    }

    public String getHikvisionUuid() {
        return hikvisionUuid;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }

    public String getPlate() {
        return plate;
    }

    public Integer getConfidenceLevel() {
        return confidenceLevel;
    }

    public Integer getPictureCount() {
        return pictureCount;
    }

    public boolean isDataRetransmission() {
        return dataRetransmission;
    }

    public HikvisionProcessingStatus getProcessingStatus() {
        return processingStatus;
    }
}
