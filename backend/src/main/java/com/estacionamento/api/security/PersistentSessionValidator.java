package com.estacionamento.api.security;
import com.estacionamento.api.domain.user.User;
import com.estacionamento.api.domain.user.UserRepository;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
@Service
public class PersistentSessionValidator implements OAuth2TokenValidator<Jwt> {
    private final UserRepository users;
    public PersistentSessionValidator(UserRepository users) { this.users = users; }
    @Transactional(readOnly = true)
    public Optional<User> current(Jwt jwt) {
        if (jwt.getExpiresAt() == null || jwt.getSubject() == null || !(jwt.getClaim("session_version") instanceof Number version)) return Optional.empty();
        final long id;
        try { id = Long.parseLong(jwt.getSubject()); }
        catch (NumberFormatException exception) { return Optional.empty(); }
        return users.findById(id).filter(user -> user.isActive() && user.getRole().isActive()
                && user.getSessionVersion() == version.longValue()
                && user.getUsername().equals(jwt.getClaimAsString("username")));
    }
    @Override
    @Transactional(readOnly = true)
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        return current(jwt).isPresent() ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Session revoked", null));
    }
}
