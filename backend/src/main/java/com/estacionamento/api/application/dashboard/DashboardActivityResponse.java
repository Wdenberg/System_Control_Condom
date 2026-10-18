package com.estacionamento.api.application.dashboard;

import com.estacionamento.api.application.movement.MovementResult;

public record DashboardActivityResponse(
        MovementResult movement,
        String activityType
) {
    public static DashboardActivityResponse from(MovementResult movement) {
        String activityType = movement.status().name();
        return new DashboardActivityResponse(movement, activityType);
    }
}
