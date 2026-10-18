package com.estacionamento.api.infrastructure.hikvision;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlateNormalizerTest {

    private final PlateNormalizer normalizer = new PlateNormalizer();

    @Test
    void shouldNormalizePlateWithoutSeparators() {
        assertThat(normalizer.normalize("abc-1234")).isEqualTo("ABC1234");
        assertThat(normalizer.normalize(" abc1d23 ")).isEqualTo("ABC1D23");
    }
}
