package com.estacionamento.api.application.hikvision;

import com.estacionamento.api.application.movement.MovementResult;
import com.estacionamento.api.application.movement.MovementService;
import com.estacionamento.api.application.realtime.RealtimeEventPublisher;
import com.estacionamento.api.config.properties.HikvisionProperties;
import com.estacionamento.api.domain.camera.Camera;
import com.estacionamento.api.domain.camera.CameraRepository;
import com.estacionamento.api.domain.hikvision.HikvisionEvent;
import com.estacionamento.api.domain.hikvision.HikvisionEventRepository;
import com.estacionamento.api.domain.hikvision.HikvisionPicture;
import com.estacionamento.api.domain.hikvision.HikvisionPictureRepository;
import com.estacionamento.api.domain.hikvision.HikvisionProcessingStatus;
import com.estacionamento.api.infrastructure.hikvision.HikvisionImageStorage;
import com.estacionamento.api.infrastructure.hikvision.HikvisionXmlParser;
import com.estacionamento.api.infrastructure.hikvision.PlateNormalizer;
import com.estacionamento.api.infrastructure.hikvision.Sha256;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class HikvisionEventService {

    private static final Logger log =
            LoggerFactory.getLogger(HikvisionEventService.class);

    private final CameraRepository cameraRepository;
    private final HikvisionEventIdempotencyLock idempotencyLock;
    private final HikvisionEventRepository eventRepository;
    private final HikvisionPictureRepository pictureRepository;
    private final HikvisionXmlParser parser;
    private final PlateNormalizer plateNormalizer;
    private final HikvisionCooldownService cooldownService;
    private final HikvisionImageStorage imageStorage;
    private final HikvisionProperties properties;
    private final MovementService movementService;
    private final RealtimeEventPublisher realtimeEventPublisher;

    public HikvisionEventService(
            CameraRepository cameraRepository,
            HikvisionEventIdempotencyLock idempotencyLock,
            HikvisionEventRepository eventRepository,
            HikvisionPictureRepository pictureRepository,
            HikvisionXmlParser parser,
            PlateNormalizer plateNormalizer,
            HikvisionCooldownService cooldownService,
            HikvisionImageStorage imageStorage,
            HikvisionProperties properties,
            MovementService movementService,
            RealtimeEventPublisher realtimeEventPublisher
    ) {
        this.cameraRepository = cameraRepository;
        this.idempotencyLock = idempotencyLock;
        this.eventRepository = eventRepository;
        this.pictureRepository = pictureRepository;
        this.parser = parser;
        this.plateNormalizer = plateNormalizer;
        this.cooldownService = cooldownService;
        this.imageStorage = imageStorage;
        this.properties = properties;
        this.movementService = movementService;
        this.realtimeEventPublisher = realtimeEventPublisher;
    }

    @Transactional
    public HikvisionEventResponse process(
            String rawXml,
            String remoteIpAddress,
            List<HikvisionAttachment> attachments
    ) {
        validatePayload(rawXml);

        List<HikvisionAttachment> safeAttachments =
                attachments == null
                        ? List.of()
                        : List.copyOf(attachments);

        ParsedHikvisionAnprEvent parsed = parser.parse(rawXml);
        if (parsed.confidenceLevel() != null && (parsed.confidenceLevel() < 0 || parsed.confidenceLevel() > 100)) throw new IllegalArgumentException("Confidence must be between 0 and 100");

        log.info(
                "Hikvision ANPR parsed: cameraIp={}, remoteIp={}, plate={}, " +
                        "confidence={}, eventType={}, eventState={}, occurredAt={}, uuid={}",
                parsed.sourceIpAddress(),
                remoteIpAddress,
                parsed.plate(),
                parsed.confidenceLevel(),
                parsed.eventType(),
                parsed.eventState(),
                parsed.occurredAt(),
                parsed.uuid()
        );

        Camera camera = findActiveCamera(parsed.sourceIpAddress());

        String plate = normalizePlate(parsed.plate());

        String externalEventId = resolveExternalEventId(
                parsed,
                plate,
                rawXml
        );

        idempotencyLock.lock(String.valueOf(camera.getId()), externalEventId);
        var existingEvent = eventRepository
                .findByCamera_IdAndExternalEventId(
                        camera.getId(),
                        externalEventId
                );

        if (existingEvent.isPresent()) {
            var prior = existingEvent.get();
            if (!java.util.Objects.equals(prior.getPlate(), plate) || !prior.getOccurredAt().toInstant().equals(parsed.occurredAt().toInstant()))
                throw new com.estacionamento.api.application.shared.DuplicateResourceException("Event identifier was already used for a different recognition");
            return buildDuplicateResponse(
                    existingEvent.get(),
                    externalEventId,
                    camera,
                    plate,
                    parsed
            );
        }

        boolean cooldownAcquired =
                acquireCooldown(
                        parsed.sourceIpAddress(),
                        plate
                );

        HikvisionProcessingStatus status =
                cooldownAcquired
                        ? HikvisionProcessingStatus.RECEIVED
                        : HikvisionProcessingStatus.COOLDOWN;

        HikvisionEvent event = createEvent(
                parsed,
                camera,
                remoteIpAddress,
                plate,
                externalEventId,
                status,
                rawXml
        );

        event = eventRepository.saveAndFlush(event);

        persistPictures(
                event,
                parsed,
                safeAttachments
        );

        MovementResult movement = processMovement(
                status,
                event,
                camera,
                plate,
                parsed
        );

        HikvisionEventResponse response = HikvisionEventResponse.of(
                event.getId(),
                status,
                externalEventId,
                camera.getName(),
                camera.getDirection(),
                plate,
                parsed.confidenceLevel(),
                parsed.occurredAt(),
                parsed.pictures().size(),
                movement
        );

        realtimeEventPublisher.publish(
                "hikvision.received",
                response
        );

        log.info(
                "Hikvision event processed: eventId={}, externalEventId={}, " +
                        "camera={}, cameraDirection={}, plate={}, status={}",
                event.getId(),
                externalEventId,
                camera.getName(),
                camera.getDirection(),
                plate,
                status
        );

        return response;
    }

    private void validatePayload(String rawXml) {
        if (rawXml == null || rawXml.isBlank()) {
            throw new IllegalArgumentException(
                    "Hikvision XML payload cannot be empty"
            );
        }
    }

    private Camera findActiveCamera(String cameraIp) {
        return cameraRepository
                .findByIpAddressAndEnabledTrue(cameraIp)
                .orElseThrow(() ->
                        new HikvisionUnknownCameraException(cameraIp)
                );
    }

    private String normalizePlate(String rawPlate) {
        String plate = plateNormalizer.normalize(rawPlate);

        if (plate == null || plate.isBlank()) {
            throw new IllegalArgumentException(
                    "Hikvision ANPR event contains an empty license plate"
            );
        }

        return plate;
    }

    private String resolveExternalEventId(
            ParsedHikvisionAnprEvent parsed,
            String plate,
            String rawXml
    ) {
        if (parsed.uuid() != null && !parsed.uuid().isBlank()) {
            return parsed.uuid().trim();
        }

        return Sha256.hex(
                parsed.sourceIpAddress()
                        + "|"
                        + parsed.occurredAt()
                        + "|"
                        + plate
                        + "|"
                        + rawXml
        );
    }

    private HikvisionEventResponse buildDuplicateResponse(
            HikvisionEvent existingEvent,
            String externalEventId,
            Camera camera,
            String plate,
            ParsedHikvisionAnprEvent parsed
    ) {
        HikvisionEventResponse response = HikvisionEventResponse.of(
                existingEvent.getId(),
                HikvisionProcessingStatus.DUPLICATE,
                externalEventId,
                camera.getName(),
                camera.getDirection(),
                plate,
                parsed.confidenceLevel(),
                parsed.occurredAt(),
                parsed.pictures().size()
        );

        realtimeEventPublisher.publish(
                "hikvision.received",
                response
        );

        log.info(
                "Duplicate Hikvision event ignored: eventId={}, " +
                        "externalEventId={}, camera={}, plate={}",
                existingEvent.getId(),
                externalEventId,
                camera.getName(),
                plate
        );

        return response;
    }

    private boolean acquireCooldown(
            String sourceIpAddress,
            String plate
    ) {
        try {
            return cooldownService.acquire(
                    sourceIpAddress,
                    plate
            );
        } catch (RuntimeException exception) {
            /*
             * Redis é apenas uma otimização.
             * A idempotência definitiva permanece no PostgreSQL.
             */
            log.warn(
                    "Hikvision cooldown unavailable. Continuing without cooldown: " +
                            "cameraIp={}, plate={}, message={}",
                    sourceIpAddress,
                    plate,
                    exception.getMessage()
            );

            return true;
        }
    }

    private HikvisionEvent createEvent(
            ParsedHikvisionAnprEvent parsed,
            Camera camera,
            String remoteIpAddress,
            String plate,
            String externalEventId,
            HikvisionProcessingStatus status,
            String rawXml
    ) {
        return new HikvisionEvent(
                parsed.sourceIpAddress(),
                remoteIpAddress,
                externalEventId,
                parsed.uuid(),
                parsed.eventType(),
                parsed.eventState(),
                parsed.eventDescription(),
                parsed.channelId(),
                parsed.channelName(),
                parsed.macAddress(),
                parsed.occurredAt(),
                plate,
                parsed.originalLicensePlate(),
                parsed.countryCode(),
                parsed.lineNumber(),
                parsed.captureDirection(),
                parsed.confidenceLevel(),
                parsed.plateType(),
                parsed.plateColor(),
                parsed.licenseBright(),
                parsed.dangmark(),
                parsed.twoWheelVehicle(),
                parsed.threeWheelVehicle(),
                parsed.plateCharBelieve(),
                parsed.vehicleType(),
                parsed.detectDir(),
                parsed.detectType(),
                parsed.alarmDataType(),
                parsed.vehicleIndex(),
                parsed.vehicleColorDepth(),
                parsed.vehicleColor(),
                parsed.vehicleLength(),
                parsed.vehicleLogoRecog(),
                parsed.vehicleSubLogoRecog(),
                parsed.vehicleModel(),
                parsed.vehicleListName(),
                parsed.plateSize(),
                resolvePictureCount(parsed),
                parsed.dataRetransmission(),
                Sha256.hex(rawXml),
                status,
                null,
                rawXml,
                camera
        );
    }

    private Integer resolvePictureCount(
            ParsedHikvisionAnprEvent parsed
    ) {
        return parsed.pictureCount() != null
                ? parsed.pictureCount()
                : parsed.pictures().size();
    }

    private void persistPictures(
            HikvisionEvent event,
            ParsedHikvisionAnprEvent parsed,
            List<HikvisionAttachment> attachments
    ) {
        if (parsed.pictures() == null || parsed.pictures().isEmpty()) {
            return;
        }

        List<String> storedPaths = new ArrayList<>();

        try {
            for (var picturePayload : parsed.pictures()) {

                HikvisionPicture picture = new HikvisionPicture(
                        event,
                        picturePayload.fileName(),
                        picturePayload.type(),
                        picturePayload.dataType(),
                        picturePayload.picRecogMode(),
                        picturePayload.absoluteTime(),
                        picturePayload.pictureId(),
                        picturePayload.pictureUrl(),
                        picturePayload.plateRect() == null
                                ? null
                                : picturePayload.plateRect().x(),
                        picturePayload.plateRect() == null
                                ? null
                                : picturePayload.plateRect().y(),
                        picturePayload.plateRect() == null
                                ? null
                                : picturePayload.plateRect().width(),
                        picturePayload.plateRect() == null
                                ? null
                                : picturePayload.plateRect().height()
                );

                HikvisionAttachment attachment =
                        findAttachment(
                                picturePayload.fileName(),
                                attachments
                        );

                if (attachment != null
                        && picturePayload.dataType() != null
                        && picturePayload.dataType() == 0) {

                    HikvisionImageStorage.StoredFile storedFile =
                            imageStorage.store(
                                    event.getId(),
                                    picturePayload,
                                    attachment
                            );

                    if (storedFile != null) {
                        picture.attachStoredFile(
                                storedFile.relativePath(),
                                storedFile.contentType(),
                                storedFile.size(),
                                storedFile.sha256()
                        );

                        storedPaths.add(storedFile.relativePath());
                        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                            new org.springframework.transaction.support.TransactionSynchronization() {
                                @Override public void afterCompletion(int outcome) {
                                    if (outcome != STATUS_COMMITTED) imageStorage.deleteIfExists(storedFile.relativePath());
                                }
                            });
                    }
                }

                pictureRepository.save(picture);
            }

        } catch (RuntimeException exception) {

            storedPaths.forEach(
                    imageStorage::deleteIfExists
            );

            log.error(
                    "Failed to persist Hikvision pictures: eventId={}",
                    event.getId(),
                    exception
            );

            throw exception;
        }
    }

    private MovementResult processMovement(
            HikvisionProcessingStatus status,
            HikvisionEvent event,
            Camera camera,
            String plate,
            ParsedHikvisionAnprEvent parsed
    ) {
        if (status != HikvisionProcessingStatus.RECEIVED) {
            return null;
        }

        log.info(
                "Processing automatic movement: eventId={}, cameraId={}, " +
                        "cameraDirection={}, plate={}, confidence={}, occurredAt={}",
                event.getId(),
                camera.getId(),
                camera.getDirection(),
                plate,
                parsed.confidenceLevel(),
                parsed.occurredAt()
        );

        MovementResult result =
                movementService.processAutomatic(
                        event.getId(),
                        camera.getId(),
                        plate,
                        parsed.confidenceLevel(),
                        parsed.occurredAt()
                );

        log.info(
                "Automatic movement processed: eventId={}, result={}",
                event.getId(),
                result
        );

        return result;
    }

    private HikvisionAttachment findAttachment(
            String expectedFilename,
            List<HikvisionAttachment> attachments
    ) {
        if (attachments == null || attachments.isEmpty()) {
            return null;
        }

        return attachments.stream()
                .filter(attachment ->
                        equalsIgnoreCase(
                                expectedFilename,
                                attachment.partName()
                        )
                                || equalsIgnoreCase(
                                expectedFilename,
                                attachment.originalFilename()
                        )
                )
                .findFirst()
                .orElse(null);
    }

    private boolean equalsIgnoreCase(
            String left,
            String right
    ) {
        return left != null
                && right != null
                && left.equalsIgnoreCase(right);
    }
}
