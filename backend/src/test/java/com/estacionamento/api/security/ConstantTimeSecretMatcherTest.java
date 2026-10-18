package com.estacionamento.api.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConstantTimeSecretMatcherTest {

    @Test
    void shouldMatchExactSecret() {
        assertThat(ConstantTimeSecretMatcher.matches("secret", "secret")).isTrue();
    }

    @Test
    void shouldRejectDifferentSecret() {
        assertThat(ConstantTimeSecretMatcher.matches("secret", "Secret")).isFalse();
    }

    @Test
    void shouldRejectNullValues() {
        assertThat(ConstantTimeSecretMatcher.matches(null, "secret")).isFalse();
        assertThat(ConstantTimeSecretMatcher.matches("secret", null)).isFalse();
    }
}
