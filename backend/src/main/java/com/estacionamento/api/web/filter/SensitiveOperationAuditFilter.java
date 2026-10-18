package com.estacionamento.api.web.filter;

import com.estacionamento.api.application.audit.AuditService;
import com.estacionamento.api.domain.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
public class SensitiveOperationAuditFilter extends OncePerRequestFilter {
    private final AuditService audit;
    private final UserRepository users;
    public SensitiveOperationAuditFilter(AuditService audit, UserRepository users) { this.audit = audit; this.users = users; }
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !java.util.Set.of("POST", "PUT", "PATCH", "DELETE").contains(request.getMethod())
                || !request.getRequestURI().startsWith("/api/v1/");
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        boolean completed = false;
        try { chain.doFilter(request, response); completed = true; }
        finally {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            var actor = authentication == null || !authentication.isAuthenticated()
                    || authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken
                    ? null : users.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
            String path = request.getRequestURI();
            String action = path.startsWith("/api/v1/users") ? "USER_ADMINISTRATION"
                    : path.endsWith("/revoke") ? "SESSION_REVOCATION"
                    : path.endsWith("/login") ? "LOGIN_ATTEMPT"
                    : path.startsWith("/api/v1/gates") ? "GATE_OPERATION"
                    : path.startsWith("/api/v1/integrations") ? "WEBHOOK_ATTEMPT" : "DATA_CHANGE";
            audit.recordAttempt(actor, action, null, completed && response.getStatus() < 400,
                    "method=" + request.getMethod() + ", status=" + (completed ? response.getStatus() : 500));
        }
    }
}
