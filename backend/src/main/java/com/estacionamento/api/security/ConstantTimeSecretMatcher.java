package com.estacionamento.api.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class ConstantTimeSecretMatcher {
    private ConstantTimeSecretMatcher() {
    }

    public static boolean matches(String configured, String provided) {
        if (configured == null || provided == null) {
            return false;
        }
        return MessageDigest.isEqual(
                configured.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8)
        );
    }
}
