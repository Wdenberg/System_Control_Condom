package com.estacionamento.api.security;
import com.estacionamento.api.config.properties.WebhookCredentials;
import com.estacionamento.api.domain.camera.CameraRepository;
import com.estacionamento.api.web.hikvision.HikvisionWebhookUnauthorizedException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@EnableConfigurationProperties(WebhookCredentials.class)
public class HikvisionWebhookSecurityService {
    public static final String TOKEN_HEADER = "X-Hikvision-Webhook-Token";
    private final WebhookCredentials credentials;
    private final CameraRepository cameras;
    public HikvisionWebhookSecurityService(WebhookCredentials credentials, CameraRepository cameras) {
        this.credentials = credentials; this.cameras = cameras;
    }
    @Transactional(readOnly = true)
    public void validate(String token, String remoteIp, String cameraIp) {
        // XML identifies the claimed camera; only its distinct secret authenticates it.
        if (token == null || token.isBlank() || cameraIp == null || cameraIp.isBlank()) reject();
        var camera = cameras.findByIpAddressAndEnabledTrue(cameraIp)
                .orElseThrow(() -> new HikvisionWebhookUnauthorizedException("Invalid camera credentials"));
        String expected = credentials.tokens().get(camera.getName().toUpperCase(java.util.Locale.ROOT));
        if (expected == null || expected.isBlank() || !ConstantTimeSecretMatcher.matches(expected, token)) reject();
    }
    private static void reject() { throw new HikvisionWebhookUnauthorizedException("Invalid camera credentials"); }
}
