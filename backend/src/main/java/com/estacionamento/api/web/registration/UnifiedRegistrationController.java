package com.estacionamento.api.web.registration;
import com.estacionamento.api.application.registration.UnifiedRegistrationService;
import com.estacionamento.api.security.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/registrations")
public class UnifiedRegistrationController {
    private final UnifiedRegistrationService service;
    private final CurrentUserService users;
    public UnifiedRegistrationController(UnifiedRegistrationService service,CurrentUserService users) { this.service=service;this.users=users; }
    @PostMapping
    @PreAuthorize("hasAuthority('APARTMENT_WRITE') and hasAuthority('PARKING_WRITE') and hasAuthority('VEHICLE_WRITE')")
    public UnifiedRegistrationService.Result create(@Valid @RequestBody UnifiedRegistrationRequest request,Authentication authentication) {
        return service.create(request,users.require(authentication));
    }
}
