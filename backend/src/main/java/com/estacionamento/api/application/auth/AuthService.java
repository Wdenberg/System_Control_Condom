package com.estacionamento.api.application.auth;

import com.estacionamento.api.security.AuthenticatedUser;
import com.estacionamento.api.security.JwtTokenService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;

    public AuthService(AuthenticationManager authenticationManager, JwtTokenService jwtTokenService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
    }

    @Transactional(readOnly = true)
    public AuthenticatedLogin authenticate(String username, String password) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );

        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return new AuthenticatedLogin(user, jwtTokenService.generate(user));
    }

    public record AuthenticatedLogin(AuthenticatedUser user, JwtTokenService.TokenData token) {
    }
}
