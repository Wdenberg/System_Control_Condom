package com.estacionamento.api.web.camera;

import com.estacionamento.api.domain.camera.Camera;
import com.estacionamento.api.domain.camera.CameraDirection;

public record CameraResponse(
        Long id,
        String name,
        String ipAddress,
        CameraDirection direction,
        String vendor,
        String model,
        Integer channelId,
        boolean enabled
) {
    public static CameraResponse from(Camera camera) {
        return new CameraResponse(
                camera.getId(), camera.getName(), camera.getIpAddress(), camera.getDirection(),
                camera.getVendor(), camera.getModel(), camera.getChannelId(), camera.isEnabled()
        );
    }
}
