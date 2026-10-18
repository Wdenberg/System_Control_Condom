package com.estacionamento.api.web.auth;

import java.time.Instant;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        Instant expiresAt,
        UserSummary user
) {
}
