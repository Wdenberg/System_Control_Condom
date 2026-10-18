package com.estacionamento.api.web.filter;

import com.estacionamento.api.application.audit.AuditRequestContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";

    private final ClientIpResolver clientIpResolver;

    public CorrelationIdFilter(ClientIpResolver clientIpResolver) {
        this.clientIpResolver = clientIpResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String supplied = request.getHeader(HEADER);
        String correlationId = normalize(supplied);
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }

        response.setHeader(HEADER, correlationId);
        MDC.put("correlationId", correlationId);
        AuditRequestContext.set(new AuditRequestContext.Context(
                correlationId,
                request.getMethod(),
                request.getRequestURI(),
                clientIpResolver.resolve(request),
                trim(request.getHeader("User-Agent"), 512)
        ));

        try {
            filterChain.doFilter(request, response);
        } finally {
            AuditRequestContext.clear();
            MDC.remove("correlationId");
        }
    }

    private static String normalize(String value) {
        String trimmed = trim(value, 64);
        return trimmed == null || trimmed.isBlank() ? null : trimmed;
    }

    private static String trim(String value, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }
}
