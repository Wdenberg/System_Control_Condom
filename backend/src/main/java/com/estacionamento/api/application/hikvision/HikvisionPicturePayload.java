package com.estacionamento.api.application.hikvision;

public record HikvisionPicturePayload(
        String fileName,
        String type,
        Integer dataType,
        Integer picRecogMode,
        String absoluteTime,
        String pictureId,
        String pictureUrl,
        PlateRectPayload plateRect
) {
    public record PlateRectPayload(Integer x, Integer y, Integer width, Integer height) {
    }
}
