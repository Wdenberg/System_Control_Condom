package com.estacionamento.api.security;
import com.estacionamento.api.domain.user.*;
import com.estacionamento.api.domain.role.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import java.time.Instant;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class PersistentSessionValidatorTest {
    private final UserRepository users = mock(UserRepository.class);
    private final PersistentSessionValidator validator = new PersistentSessionValidator(users);
    private Jwt token(long version) { return Jwt.withTokenValue("test").header("alg", "HS256").subject("1").claim("username", "operator").claim("session_version", version).issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build(); }
    private User user() { User user = new User("operator", "hash", "Operator", new Role(RoleCode.OPERATOR)); when(users.findById(1L)).thenReturn(Optional.of(user)); return user; }
    @Test void acceptsCurrentSession() { user(); assertThat(validator.validate(token(0)).hasErrors()).isFalse(); }
    @Test void rejectsAfterPasswordReset() { user().changePassword("other-hash"); assertThat(validator.validate(token(0)).hasErrors()).isTrue(); }
    @Test void rejectsAfterRoleChange() { user().changeRole(new Role(RoleCode.VIEWER)); assertThat(validator.validate(token(0)).hasErrors()).isTrue(); }
    @Test void rejectsDisabledUser() { user().setActive(false); assertThat(validator.validate(token(0)).hasErrors()).isTrue(); }
    @Test void rejectsExplicitRevocationAcrossSessions() { user().revokeSessions(); assertThat(validator.validate(token(0)).hasErrors()).isTrue(); }
    @Test void rejectsMissingUser() { assertThat(validator.validate(token(0)).hasErrors()).isTrue(); }
}
