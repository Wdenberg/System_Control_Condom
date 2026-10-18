package com.estacionamento.api.web.filter;

import com.estacionamento.api.config.properties.SecurityHardeningProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class ClientIpResolver {

    private final SecurityHardeningProperties properties;
    private final java.util.Set<String> trustedProxies;

    public ClientIpResolver(SecurityHardeningProperties properties) { this(properties, ""); }
    @org.springframework.beans.factory.annotation.Autowired
    public ClientIpResolver(SecurityHardeningProperties properties,
            @org.springframework.beans.factory.annotation.Value("${app.security.hardening.trusted-proxies:}") String trustedProxies) {
        this.properties = properties;
        this.trustedProxies = java.util.Arrays.stream(trustedProxies.split(",")).map(String::trim).filter(value -> !value.isBlank()).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public String resolve(HttpServletRequest request) {
        if (properties.trustForwardedHeaders() && trustedProxies.contains(request.getRemoteAddr())) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return normalize(forwarded.split(",", 2)[0]);
            }
            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isBlank()) {
                return normalize(realIp);
            }
        }
        return normalize(request.getRemoteAddr());
    }

    private static String normalize(String value) {
        if (value == null) return "unknown";
        String trimmed = value.trim();
        return trimmed.length() <= 45 ? trimmed : trimmed.substring(0, 45);
    }
}
