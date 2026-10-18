package com.estacionamento.api.infrastructure.hikvision;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;

@Component
public class PlateNormalizer {

    public String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]", "");
        if (normalized.isBlank()) {
            return null;
        }
        return normalized;
    }
}
