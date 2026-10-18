package com.estacionamento.api.domain.hikvision;

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
import jakarta.persistence.UniqueConstraint;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(
        name = "hikvision_pictures",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_hikvision_picture_event_pid_filename",
                columnNames = {"hikvision_event_id", "picture_id", "file_name"}
        )
)
public class HikvisionPicture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hikvision_event_id", nullable = false)
    private HikvisionEvent event;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(nullable = false, length = 64)
    private String type;

    @Column(name = "data_type")
    private Integer dataType;

    @Column(name = "pic_recog_mode")
    private Integer picRecogMode;

    @Column(name = "absolute_time", length = 32)
    private String absoluteTime;

    @Column(name = "picture_id", length = 128)
    private String pictureId;

    @Column(name = "picture_url", length = 1000)
    private String pictureUrl;

    @Column(name = "plate_x")
    private Integer plateX;

    @Column(name = "plate_y")
    private Integer plateY;

    @Column(name = "plate_width")
    private Integer plateWidth;

    @Column(name = "plate_height")
    private Integer plateHeight;

    @Column(name = "storage_path", length = 1000)
    private String storagePath;

    @Column(name = "content_type", length = 128)
    private String contentType;

    @Column(name = "content_size")
    private Long contentSize;

    @Column(name = "content_sha256", length = 64)
    private String contentSha256;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected HikvisionPicture() {
    }

    public HikvisionPicture(HikvisionEvent event, String fileName, String type, Integer dataType,
                            Integer picRecogMode, String absoluteTime, String pictureId, String pictureUrl,
                            Integer plateX, Integer plateY, Integer plateWidth, Integer plateHeight) {
        this.event = event;
        this.fileName = fileName;
        this.type = type;
        this.dataType = dataType;
        this.picRecogMode = picRecogMode;
        this.absoluteTime = absoluteTime;
        this.pictureId = pictureId;
        this.pictureUrl = pictureUrl;
        this.plateX = plateX;
        this.plateY = plateY;
        this.plateWidth = plateWidth;
        this.plateHeight = plateHeight;
    }

    public void attachStoredFile(String storagePath, String contentType, long contentSize, String contentSha256) {
        this.storagePath = storagePath;
        this.contentType = contentType;
        this.contentSize = contentSize;
        this.contentSha256 = contentSha256;
    }

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
