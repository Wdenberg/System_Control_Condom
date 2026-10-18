package com.estacionamento.api.security;

import com.estacionamento.api.config.properties.JwtProperties;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;
    private final Clock clock;

    public JwtTokenService(JwtEncoder jwtEncoder, JwtProperties properties, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    public TokenData generate(AuthenticatedUser user) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(properties.accessTokenTtl());
        List<String> authorities = user.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(String.valueOf(user.getId()))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("username", user.getUsername())
                .claim("full_name", user.getFullName())
                .claim("role", user.getRole().name())
                .claim("authorities", authorities)
                .claim("session_version", user.getSessionVersion())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256)
                .type("JWT")
                .build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenData(token, properties.accessTokenTtl().toSeconds(), expiresAt);
    }

    public record TokenData(String value, long expiresInSeconds, Instant expiresAt) {
    }
}
