package com.estacionamento.api.web.auth;

import com.estacionamento.api.application.auth.AuthService;
import com.estacionamento.api.security.AuthenticatedUser;
import com.estacionamento.api.security.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final CurrentUserService currentUserService;

    public AuthController(AuthService authService, CurrentUserService currentUserService) {
        this.authService = authService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        var result = authService.authenticate(request.username(), request.password());
        var token = result.token();
        return ResponseEntity.ok(new LoginResponse(
                token.value(),
                "Bearer",
                token.expiresInSeconds(),
                token.expiresAt(),
                UserSummary.from(result.user())
        ));
    }

    @GetMapping("/me")
    public MeResponse me(Authentication authentication) {
        return new MeResponse(UserSummary.from(AuthenticatedUser.from(currentUserService.require(authentication))));
    }
    @PostMapping("/revoke")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Void> revoke(Authentication authentication) {
        currentUserService.require(authentication).revokeSessions();
        return ResponseEntity.noContent().build();
    }
}
