package com.estacionamento.api.application.hikvision;

public record HikvisionAttachment(
        String partName,
        String originalFilename,
        String contentType,
        byte[] content
) {
}
