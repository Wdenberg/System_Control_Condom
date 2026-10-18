package com.estacionamento.api.security;
import com.estacionamento.api.config.properties.WebhookCredentials;
import com.estacionamento.api.domain.camera.*;
import com.estacionamento.api.web.hikvision.HikvisionWebhookUnauthorizedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class HikvisionWebhookSecurityServiceTest {
    private static final String ENTRY = "entry-test-secret-at-least-32-characters";
    private static final String EXIT = "exit-test-secret-at-least-32-characters";
    private final CameraRepository cameras = mock(CameraRepository.class);
    private final HikvisionWebhookSecurityService service = new HikvisionWebhookSecurityService(
            new WebhookCredentials(Map.of("CAM-ENTRADA", ENTRY, "CAM-SAIDA", EXIT)), cameras);
    private void camera() {
        when(cameras.findByIpAddressAndEnabledTrue("192.168.15.43")).thenReturn(Optional.of(
                new Camera("CAM-ENTRADA", "192.168.15.43", CameraDirection.ENTRY, "HIKVISION", "test", 1, true)));
    }
    @Test void shouldAcceptValidToken() { camera(); assertThatCode(() -> service.validate(ENTRY, "untrusted-ip", "192.168.15.43")).doesNotThrowAnyException(); }
    @Test void shouldRejectInvalidToken() { camera(); assertThatThrownBy(() -> service.validate("wrong", "192.168.15.43", "192.168.15.43")).isInstanceOf(HikvisionWebhookUnauthorizedException.class); }
    @Test void shouldRejectConfiguredCameraWhenTokenIsNotConfigured() {
        camera(); var closed = new HikvisionWebhookSecurityService(new WebhookCredentials(Map.of()), cameras);
        assertThatThrownBy(() -> closed.validate(ENTRY, "192.168.15.43", "192.168.15.43")).isInstanceOf(HikvisionWebhookUnauthorizedException.class);
    }
    @Test void shouldRejectUnknownCameraWhenTokenIsNotConfigured() { assertThatThrownBy(() -> service.validate(ENTRY, "192.168.15.43", "192.168.15.99")).isInstanceOf(HikvisionWebhookUnauthorizedException.class); }
    @Test void shouldRejectWhenRemoteIpAndCameraIpAreMissing() { assertThatThrownBy(() -> service.validate(ENTRY, null, null)).isInstanceOf(HikvisionWebhookUnauthorizedException.class); }
    @ParameterizedTest @NullAndEmptySource void rejectsMissingOrEmptyToken(String token) { assertThatThrownBy(() -> service.validate(token, "192.168.15.43", "192.168.15.43")).isInstanceOf(HikvisionWebhookUnauthorizedException.class); }
    @Test void rejectsOtherCameraTokenAndSpoofedIp() { camera(); assertThatThrownBy(() -> service.validate(EXIT, "192.168.15.43", "192.168.15.43")).isInstanceOf(HikvisionWebhookUnauthorizedException.class); }
    @Test void rejectsDisabledCamera() { assertThatThrownBy(() -> service.validate(ENTRY, "192.168.15.43", "192.168.15.43")).isInstanceOf(HikvisionWebhookUnauthorizedException.class); }
    @Test void rejectsSharedOrShortCredentials() { assertThatThrownBy(() -> new WebhookCredentials(Map.of("a", ENTRY, "b", ENTRY))).isInstanceOf(IllegalArgumentException.class); assertThatThrownBy(() -> new WebhookCredentials(Map.of("a", "short"))).isInstanceOf(IllegalArgumentException.class); }
}
