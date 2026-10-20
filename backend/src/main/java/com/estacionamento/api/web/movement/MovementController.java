package com.estacionamento.api.web.movement;

import com.estacionamento.api.application.movement.MovementResult;
import com.estacionamento.api.application.movement.MovementService;
import com.estacionamento.api.security.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/movements")
public class MovementController {

    private final MovementService movementService;
    private final CurrentUserService currentUserService;

    public MovementController(MovementService movementService, CurrentUserService currentUserService) {
        this.movementService = movementService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('MOVEMENT_READ')")
    public Page<MovementResult> list(@PageableDefault(size = 30) Pageable pageable) {
        return movementService.list(pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('MOVEMENT_READ')")
    public MovementResult get(@PathVariable Long id) {
        return movementService.get(id);
    }

    @PostMapping("/manual")
    @PreAuthorize("hasAuthority('MOVEMENT_MANUAL_WRITE')")
    public ResponseEntity<MovementResult> manual(@Valid @RequestBody ManualMovementRequest request,
                                                  Authentication authentication) {
        return ResponseEntity.ok(movementService.createManual(
                request.plate(), request.direction(), request.justification(), currentUserService.require(authentication).getId()));
    }

    @PostMapping("/{id}/override")
    @PreAuthorize("hasAuthority('MOVEMENT_OVERRIDE')")
    public ResponseEntity<MovementResult> override(@PathVariable Long id,
                                                    @Valid @RequestBody OverrideMovementRequest request,
                                                    Authentication authentication) {
        return ResponseEntity.ok(movementService.override(
                id, request.justification(), currentUserService.require(authentication).getId()));
    }
    public record PassageConfirmationRequest(boolean passed, Boolean controllerSettled, @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(min = 10, max = 1000) String justification) {}
    @PostMapping("/{id}/passage-confirmation")
    @PreAuthorize("hasAuthority('MOVEMENT_OVERRIDE')")
    public com.estacionamento.api.application.movement.MovementResult confirmPassage(@PathVariable Long id,
            @Valid @RequestBody PassageConfirmationRequest request, Authentication authentication) {
        return movementService.confirmPassage(id, request.passed(), request.justification(), currentUserService.require(authentication).getId(), Boolean.TRUE.equals(request.controllerSettled()));
    }
}
