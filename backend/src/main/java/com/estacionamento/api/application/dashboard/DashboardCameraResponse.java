package com.estacionamento.api.application.dashboard;

import com.estacionamento.api.domain.camera.CameraDirection;

import java.time.OffsetDateTime;

public record DashboardCameraResponse(
        Long id,
        String name,
        String ipAddress,
        CameraDirection direction,
        String vendor,
        String model,
        boolean enabled,
        OffsetDateTime lastEventAt,
        String lastPlate
) {
}
