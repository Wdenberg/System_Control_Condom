package com.estacionamento.api.infrastructure.hikvision;

import com.estacionamento.api.application.hikvision.HikvisionAttachment;
import com.estacionamento.api.config.properties.HikvisionProperties;
import com.estacionamento.api.application.hikvision.HikvisionPicturePayload;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Locale;

@Component
public class HikvisionImageStorage {

    private final HikvisionProperties properties;
    private final Clock clock;

    public HikvisionImageStorage(HikvisionProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public StoredFile store(long eventId, HikvisionPicturePayload picture, HikvisionAttachment attachment) {
        if (attachment == null || attachment.content() == null || attachment.content().length == 0) {
            return null;
        }
        if (attachment.content().length > 15 * 1024 * 1024) {
            throw new IllegalArgumentException("Hikvision image exceeds 15 MB: " + picture.fileName());
        }
        if (attachment.contentType() == null || !attachment.contentType().toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new IllegalArgumentException("Hikvision attachment is not an image: " + picture.fileName());
        }

        String extension = extensionFor(picture.fileName(), attachment.contentType());
        LocalDate date = LocalDate.now(clock);
        Path directory = properties.imageStoragePath()
                .resolve(String.valueOf(date.getYear()))
                .resolve(String.format("%02d", date.getMonthValue()))
                .resolve(String.format("%02d", date.getDayOfMonth()))
                .resolve(String.valueOf(eventId));
        try {
            Files.createDirectories(directory);
            String safeType = picture.type().replaceAll("[^A-Za-z0-9_-]", "_").toLowerCase(Locale.ROOT);
            String filename = safeType + "-" + Sha256.hex(attachment.content()).substring(0, 16) + extension;
            Path destination = directory.resolve(filename).normalize();
            if (!destination.startsWith(directory.normalize())) {
                throw new IllegalArgumentException("Unsafe image path");
            }
            Files.write(destination, attachment.content());
            String relative = properties.imageStoragePath().relativize(destination).toString().replace('\\', '/');
            return new StoredFile(relative, attachment.contentType(), attachment.content().length, Sha256.hex(attachment.content()));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not store Hikvision image", exception);
        }
    }

    public void deleteIfExists(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(properties.imageStoragePath().resolve(relativePath).normalize());
        } catch (IOException exception) {
            // Best-effort cleanup; the database transaction will still be rolled back.
        }
    }

    private String extensionFor(String fileName, String contentType) {
        String lower = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            return ".png";
        }
        if (lower.endsWith(".webp")) {
            return ".webp";
        }
        if (lower.endsWith(".jpeg")) {
            return ".jpeg";
        }
        if (contentType != null && contentType.toLowerCase(Locale.ROOT).contains("png")) {
            return ".png";
        }
        return ".jpg";
    }

    public record StoredFile(String relativePath, String contentType, long size, String sha256) {
    }
}
