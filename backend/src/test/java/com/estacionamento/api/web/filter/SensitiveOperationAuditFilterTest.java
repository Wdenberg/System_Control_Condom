package com.estacionamento.api.web.filter;

import com.estacionamento.api.application.audit.AuditService;
import com.estacionamento.api.domain.user.User;
import com.estacionamento.api.domain.user.UserRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

class SensitiveOperationAuditFilterTest {
    private final AuditService audit = mock(AuditService.class);
    private final UserRepository users = mock(UserRepository.class);
    private final SensitiveOperationAuditFilter filter = new SensitiveOperationAuditFilter(audit, users);

    @AfterEach void clearSecurityContext() { SecurityContextHolder.clearContext(); }

    @Test void anonymousAttemptsCannotBeAttributedToAnAccountNamedAnonymousUser() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "test", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
        filter.doFilter(new MockHttpServletRequest("POST", "/api/v1/auth/login"), new MockHttpServletResponse(), mock(FilterChain.class));
        verifyNoInteractions(users);
        verify(audit).recordAttempt(isNull(), eq("LOGIN_ATTEMPT"), isNull(), eq(true), anyString());
    }

    @Test void deniedAuthenticatedRequestRetainsItsActorAndFailureOutcome() throws Exception {
        User actor = mock(User.class);
        when(users.findByUsernameIgnoreCase("operator")).thenReturn(Optional.of(actor));
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                "operator", "unused", List.of(new SimpleGrantedAuthority("MOVEMENT_READ"))));
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(403);
        filter.doFilter(new MockHttpServletRequest("POST", "/api/v1/gates/test/ENTRY"), response, mock(FilterChain.class));
        verify(audit).recordAttempt(eq(actor), eq("GATE_OPERATION"), isNull(), eq(false), contains("status=403"));
    }
}
